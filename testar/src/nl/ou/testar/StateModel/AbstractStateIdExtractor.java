package nl.ou.testar.StateModel;

import nl.ou.testar.StateModel.Util.HydrationHelper;
import org.fruit.alayer.Action;
import org.fruit.alayer.State;
import org.fruit.alayer.Tags;
import java.util.ArrayList;
import java.util.List;
import static nl.ou.testar.StateModel.ExtractionMode.*;

/**
 * This class extracts the abstract state ids for use in the abstract layer of our models.
 */
public class AbstractStateIdExtractor {

    // a list of state id strings that have been extracted
    private List<String> extractedStateIds;

    // in what mode is the extractor to be run?
    private ExtractionMode extractionMode;

    private String previousStateId;

    /**
     * Constructor
     * @param extractionMode
     */
    public AbstractStateIdExtractor(ExtractionMode extractionMode) {
        this.extractionMode = extractionMode;
        if (extractionMode == ALL_STATES) {
            extractedStateIds = new ArrayList<>();
        }
        if (extractionMode == PREVIOUS_STATE || extractionMode == INCOMING_ACTION) {
            previousStateId = "";
        }
        System.out.println("Starting abstract state id extractor in mode: " + extractionMode);
    }

    public String extractAbstractStateId(State state, Action action) {
        switch (extractionMode) {
            case SINGLE_STATE:
                return extractSingleState(state);

            case PREVIOUS_STATE:
                return extractPreviousState(state);

            case ALL_STATES:
                return extractAllStates(state);

            case INCOMING_ACTION:
                return extractIncomingAction(state, action);

            default:
                // this means the extraction mode is null
                throw new RuntimeException("The abstract state id extractor was not initialized correctly");
        }
    }

    private String extractSingleState(State state) {
        return state.get(Tags.AbstractIDCustom);
    }

    private String extractPreviousState(State state) {
        String abstractStateId = HydrationHelper.lowCollisionID(previousStateId + extractSingleState(state));
        previousStateId = extractSingleState(state);
        return abstractStateId;
    }

    private String extractAllStates(State state) {
        // this needs to be implemented
        // implementation is non-trivial, as loops need to be eliminated.
        // if we do not, every model will be of infinite size
        return "";
    }

    private String extractIncomingAction(State state, Action action) {
        String actionId = action == null ? "" : action.get(Tags.AbstractIDCustom);
        String abstractStateId = HydrationHelper.lowCollisionID(previousStateId + actionId + extractSingleState(state));
        previousStateId = extractSingleState(state);
        return abstractStateId;
    }
}
