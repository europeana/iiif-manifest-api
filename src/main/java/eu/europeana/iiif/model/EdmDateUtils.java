package eu.europeana.iiif.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.http.client.utils.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Helper class to convert an EDM date string to a Java Date object
 * @author Patrick Ehlert
 * Created on 13-02-2018
 */
public final class EdmDateUtils {

    private static final Logger LOG = LogManager.getLogger(EdmDateUtils.class);

    private static final String DATEUPDATEDFORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final SimpleDateFormat UPDATEFORMAT = new SimpleDateFormat(DATEUPDATEDFORMAT, Locale.GERMAN);
    static {
        UPDATEFORMAT.setTimeZone(TimeZone.getTimeZone("GTM"));
    }

    private EdmDateUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * This converts an EDM date string to a Java Date object (timezone UTC)
     * See also https://github.com/hugomanguinhas/europeana_experiments/tree/master/blackhole
     * @param edmDate
     */
    public static Date dateStringToDate(String edmDate) {
        // try most common format first
        Date result = tryParseFormat("yyyy-MM-dd", edmDate);

        // try second common format
        if (result == null) {
            result = tryParseFormat("dd-MM-yyyy", edmDate);
        }

        LOG.debug("Parsed edmDateString {}, result {}", edmDate, result);
        return result;
    }

    public static Date updateStringToDate(String updateString) {
        Date updateDate = null;
        try {
            updateDate = UPDATEFORMAT.parse(updateString);
        } catch (ParseException e) {
            // ignore errors
        }
        return updateDate;
    }

    public static String updateDateToString(Date dateUpdate) {
        return UPDATEFORMAT.format(dateUpdate);
    }

    // Apache DateUtils can parse all three date format patterns allowed by RFC 2616
    public static Date headerStringToDate(String headerString) {
        Date headerDate = DateUtils.parseDate(headerString);
        if (null != headerDate) {
            return DateUtils.parseDate(headerString);
        } else {
            LOG.error("Error parsing header Date string");
            return null;
        }
    }

    // Formats the given date according to the RFC 1123 pattern.
    public static String headerDateToString(Date dateHeader) {
        return DateUtils.formatDate(dateHeader);
    }

    private static Date tryParseFormat(String format, String edmDate) {
        Date result = null;
        // TODO use something more robust (and thread-safe) instead of SimpleDateFormat (see EA-990)
        SimpleDateFormat formatter = new SimpleDateFormat(format, Locale.GERMAN);
        formatter.setTimeZone(TimeZone.getTimeZone("Europea/Amsterdam"));
        try {
            result = formatter.parse(edmDate);
        } catch (ParseException e) {
            // ignore errors
        }
        return result;
    }

}
