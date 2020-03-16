package eu.europeana.iiif.model;

import eu.europeana.iiif.service.exception.DataInconsistentException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sorts the webresources based on how they are linked to each other according to their nextInSequence values.
 * Note that we assume that 1) all webResources are part of 0 or 1 sequences and 2) all nextInSequence values have to
 * exist in the provided array of webresources
 *
 * Basically the algorithm is as follows:
 * 1. we iterate over all nodes and check each node if it's the start of a sequence
 * 2. for all the found start nodes, we follow the sequence up to the end node (and remove the webresources part of that
 * sequence, so we know which webresources are already processed)
 * 3. the found sequences are added to the final result (in no particular order)
 * 4. any remaining nodes should not be part of a sequence and are added last (also in no particular order)
 *
 * @author Patrick Ehlert
 * Created on 07-03-2018
 */
public final class WebResourceSorter {

    private static final Logger LOG = LogManager.getLogger(WebResourceSorter.class);

    private WebResourceSorter() {
        // private constructor to prevent initialiation
    }

    /**
     * Sorts the provided webResource array. All sequences should be listed first (in reverse sequence order), then
     * all webresources that are not part of a sequence
     * If there are multiple sequences, the order between sequences doesn't matter. Also the order between webresources
     * not part of a sequence doesn't matter.
     * @throws DataInconsistentException when
     * @return sorted array of webResources
     */
    public static List<WebResource> sort(List<WebResource> webResources) throws DataInconsistentException {
        LOG.trace("WebResources = {}", webResources);

        // to simplify/speed up processing we generate a hashmap that links all ids to the appropriate webResource object
        // and a hashmap with all the id's (keys) and nextInSequence (values)
        HashMap<String, WebResource> idsWebResources = new HashMap<>();
        HashMap<String, String> idsNextInSequence = new HashMap<>();
        for (WebResource wr : webResources) {
            String wrId = wr.getId();
            if (idsWebResources.put(wrId, wr) != null) {
                throw new DataInconsistentException("Duplicate webresource id found "+wrId);
            }
            String nextInSequence = wr.getNextInSequence();
            idsNextInSequence.put(wrId, nextInSequence);
            LOG.trace("    {} -> {} ", wrId, nextInSequence);
        }

        // find all start nodes (order doesn't matter)
        Set<String> startNodes = getSequenceStartItems(idsNextInSequence);
        LOG.trace("  StartNodes = {}", startNodes);

        // for each start node, follow the sequence down to the end node and list webresource in reverse order
        ArrayList<WebResource> result = new ArrayList<>();
        Iterator<String> startNodeIds = startNodes.iterator();
        while (startNodeIds.hasNext()) {
            String startNodeId = startNodeIds.next();
            List<WebResource> sequence = getSequence(startNodeId, idsWebResources, idsNextInSequence);
            LOG.trace("  Sequence = {}", sequence);
            result.addAll(sequence);
        }

        // add any remaining nodes (these should be isolated webresources, not part of any sequence)
        for (Map.Entry<String, WebResource> idWebResource : idsWebResources.entrySet()) {
            WebResource isolated = idWebResource.getValue();
            if (isolated.hasNextInSequence()) {
                throw new DataInconsistentException("Expected webresource "+isolated.getId()+" to not have a nextInSequence value");
            }
            LOG.trace("  Adding isolated node = {}", isolated);
            result.add(isolated);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Webresources = {}", result);
        }
        return result;
    }

    /**
     * Iterate over the webResources and find all items that are the start points of a sequence.
     */
    private static Set<String> getSequenceStartItems(Map<String, String> idsSequences) throws DataInconsistentException {
        Set<String> result = new HashSet<>();

        Iterator<String> ids = idsSequences.keySet().iterator();
        while (ids.hasNext()) {
            String id = ids.next();
            if (isStartOfSequence(id, idsSequences)) {
                result.add(id);
            }
        }
        return result;
    }


    /**
     *  Check if the provided webResource is the start point of a sequence (i.e. it has a 'nextInSequence' and there
     *  is no other webResource that points to it)
     */
    private static boolean isStartOfSequence(String webResourceId, Map<String, String> idsSequences) throws DataInconsistentException {
        // check if it has a nextInSequence
        String nextInSequenceId = idsSequences.get(webResourceId);
        if (StringUtils.isNotEmpty(nextInSequenceId)) {
            // verify the nextInSequence webresource is available
            if (!idsSequences.keySet().contains(nextInSequenceId)) {
                throw new DataInconsistentException("Inconsistent data: webresource " +webResourceId+ " hasNextInSequence "
                        +nextInSequenceId+ " but that webresource cannot be found!");
            }
            // check if no other webResources point to this webResource
            return !idsSequences.values().contains(webResourceId);
        }
        return false;
    }

    /**
     * Returns the entire sequence that starts in the provided startNode, in reverse order.
     * Note that we remove all the webresources we found from the provided maps so we 1) know which ones we already
     * processed later and 2) can check data consistency
     */
    private static ArrayList<WebResource> getSequence(String startNodeId,
                                                      Map<String, WebResource> idsWebResources,
                                                      Map<String, String> idsNextInSequence) throws DataInconsistentException {
        ArrayList<WebResource> result = new ArrayList<>();
        String nodeId = startNodeId;
        do {
            WebResource wr = idsWebResources.remove(nodeId);
            if (wr == null) {
                throw new DataInconsistentException("Unable to find webresource " + startNodeId + ". Most likely it's part of another sequence");
            } else {
                result.add(0, wr);
            }
            nodeId = idsNextInSequence.remove(nodeId);
        } while(nodeId != null);
        return result;
    }

}
