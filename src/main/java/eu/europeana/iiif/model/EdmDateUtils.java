package eu.europeana.iiif.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
