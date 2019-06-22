package eu.europeana.iiif.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Helper class to convert an EDM date string to a Java LocalDate object
 * @author Patrick Ehlert
 * Created on 13-02-2018
 */
public final class EdmDateUtils {

    // TODO make EdmDateStringToDate handle more different date strings (see EA-990)

    private static final Logger LOG = LogManager.getLogger(EdmDateUtils.class);

    private static final DateTimeFormatter DATE_YEARFIRST = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_YEARLAST = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private EdmDateUtils() {
        // empty constructor to prevent initialization
    }


    /**
     * Parses record timestamp_update and timestamp_created values to a ZonedDateTime object
     * @param recordDateString
     * @return
     */
    public static ZonedDateTime recordTimestampToDateTime(String recordDateString) {
        return ZonedDateTime.parse(recordDateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * This converts an EDM date string to a LocalDate object (so no timezone information is available)
     * Note that there can be a lot of different format, most of which we don't support yet
     * See also https://github.com/hugomanguinhas/europeana_experiments/tree/master/blackhole
     * @param edmDate
     */
    public static LocalDate dateStringToDate(String edmDate) {
        // try most common format first
        LocalDate result = tryParseFormat(edmDate, DATE_YEARFIRST, false);

        // try second common format
        if (result == null) {
            result = tryParseFormat(edmDate, DATE_YEARLAST, false);
        }

        LOG.debug("Parsed edmDateString {}, result {}", edmDate, result);
        return result;
    }

    private static LocalDate tryParseFormat(String edmDate, DateTimeFormatter format, boolean logError) {
        try {
            return LocalDate.parse(edmDate, format);
        } catch (Exception e) {
            if (logError) {
                LOG.error("Error parsing date {} (used formatter = {})", edmDate, format);
            }
        }
        return null;
    }

}
