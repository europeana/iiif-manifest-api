package eu.europeana.iiif.service;

import eu.europeana.iiif.web.ManifestController;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Utility class to facilitate handling If-Modified-Since, If-None-Match and If-Match request caching
 * @author Patrick Ehlert
 * Created on 03-10-2018
 */
public class CacheUtils {

    private CacheUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * Generates an eTag surrounded with double quotes
     * @param data
     * @param weakETag if true then the eTag will start with W/
     * @return
     */
    public static String generateETag(String data, boolean weakETag) {
        String eTag = "\"" + getSHA256Hash(data) + "\"";
        if (weakETag) {
            return "W/"+eTag;
        }
        return eTag;
    }

    /**
     * Generate the default headers for sending a response with caching
     * @param cacheControl optional, if not null then a Cache-Control header is added
     * @param eTag optional, if not null then an eTag header is added
     * @param lastModified optional, if not null then a Last-Modified header is added
     * @param vary optional, if not null, then a Vary header is added
     * @return
     */
    public static HttpHeaders generateCacheHeaders(String cacheControl, String eTag, ZonedDateTime lastModified, String vary) {
        HttpHeaders headers = new HttpHeaders();
        // TODO move Cache control to the Spring Boot security configuration when that's implemented
        if (cacheControl != null) {
            headers.add("Cache-Control", cacheControl);
        }
        if (eTag != null) {
            headers.add("eTag", eTag);
        }
        if (lastModified != null) {
            headers.add("Last-Modified", headerDateToString(lastModified));
        }
        if (vary != null) {
            // using the Vary header is debatable: https://www.smashingmagazine.com/2017/11/understanding-vary-header/
            headers.add("Vary", vary);
        }
        return headers;
    }

    /**
     * Formats the given date according to the RFC 1123 pattern (e.g. Thu, 4 Oct 2018 10:34:20 GMT)
     * @param lastModified
     * @return
     */
    private static String headerDateToString(ZonedDateTime lastModified) {
        return lastModified.format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    /**
     * @param request incoming HttpServletRequest
     * @param headers headers that should be sent back in the response
     * @param lastModified ZonedDateTime that indicates the lastModified date of the requested data
     * @param eTag String with the calculated eTag of the requested data
     * @return ResponseEntity with 304 or 312 status if requested object has not changed, otherwise null
     */
    public static ResponseEntity checkCached(HttpServletRequest request, HttpHeaders headers,
                                              ZonedDateTime lastModified, String eTag) {
        // chosen this implementation instead of the 'shallow' out-of-the-box spring boot version because that does not
        // offer the advantage of saving on processing time
        ZonedDateTime requestLastModified = headerStringToDate(request.getHeader("If-Modified-Since"));
        if((requestLastModified !=null && requestLastModified.compareTo(lastModified) > 0) ||
                (StringUtils.isNotEmpty(request.getHeader("If-None-Match")) &&
                        StringUtils.equalsIgnoreCase(request.getHeader("If-None-Match"), eTag))) {
            // TODO Also we ignore possible multiple eTags for now
            return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
        } else if (StringUtils.isNotEmpty(request.getHeader("If-Match")) &&
                (!StringUtils.equalsIgnoreCase(request.getHeader("If-Match"), eTag) &&
                        !StringUtils.equalsIgnoreCase(request.getHeader("If-Match"), "*"))) {
            // Note that according to the specification we have to use strong ETags here (but for now we just ignore that)
            // see https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.24
            // TODO Also we ignore possible multiple eTags for now
            return new ResponseEntity<>(headers, HttpStatus.PRECONDITION_FAILED);
        }
        return null;
    }

    /**
     * Parses the date string received in a request header
     * @param dateString
     * @return Date
     */
    private static ZonedDateTime headerStringToDate(String dateString) {
        if (StringUtils.isEmpty(dateString)) {
            return null;
        }
        // Note that Apache DateUtils can parse all 3 date format patterns allowed by RFC 2616
        Date headerDate = DateUtils.parseDate(dateString);
        if (headerDate == null) {
            LogManager.getLogger(ManifestController.class).error("Error parsing request header Date string: {}", dateString);
            return null;
        }
        return headerDate.toInstant().atOffset(ZoneOffset.UTC).toZonedDateTime();
    }



    /**
     * Calculates SHA256 hash of a particular data string
     * @param  data String of data on which the hash is based
     * @return SHA256Hash   String
     */
    private static String getSHA256Hash(String data){
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            LogManager.getLogger(CacheUtils.class).error("Error generating SHA-265 hash from record timestamp_update", e);
        }
        return null;
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte bt : hash) {
            String hex = Integer.toHexString(0xff & bt);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
