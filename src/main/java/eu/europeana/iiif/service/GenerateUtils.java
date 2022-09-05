package eu.europeana.iiif.service;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * This class was partly copied from Fulltext API, to avoid adding another dependency
 */
public class GenerateUtils {

    /**
     * Derives PageID from a media URL.
     *
     * @param mediaUrl media (target) url
     * @return MD5 hash of media url truncated to the first 5 characters
     */
    public static String derivePageId(String mediaUrl) {
        // truncate hash to reduce URL length.
        // Should not be changed as this method can be used in place of fetching the pageId from the
        // database.
        return DigestUtils.sha1Hex(mediaUrl).substring(0,7);
    }
}
