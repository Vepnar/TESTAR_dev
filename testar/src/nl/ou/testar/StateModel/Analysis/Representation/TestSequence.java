package nl.ou.testar.StateModel.Analysis.Representation;

import java.util.Arrays;
import java.util.stream.IntStream;

public class TestSequence {

    public static final int VERDICT_SUCCESS = 1;

    public static final int VERDICT_INTERRUPT_BY_USER = 2;

    public static final int VERDICT_INTERRUPT_BY_SYSTEM = 3;

    public static final int VERDICT_UNKNOWN = 4;

    public TestSequence(String sequenceId, String startDateTime, String numberOfSteps, int verdict) {
        this.sequenceId = sequenceId;
        this.startDateTime = startDateTime;
        this.numberOfSteps = numberOfSteps;
        if (IntStream.of(VERDICT_SUCCESS, VERDICT_INTERRUPT_BY_USER, VERDICT_INTERRUPT_BY_SYSTEM).anyMatch(x -> x == verdict)) {
            this.verdict = verdict;
        }
        else {
            this.verdict = VERDICT_UNKNOWN;
        }
    }

    /**
     * The id for the test sequence.
     */
    private String sequenceId;

    /**
     * The starting date and time of the sequence.
     */
    private String startDateTime;

    /**
     * The number of steps executed in this sequence.
     */
    private String numberOfSteps;

    /**
     * An integer value representing the execution verdict for this test sequence.
     */
    private int verdict;

    public String getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(String sequenceId) {
        this.sequenceId = sequenceId;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }

    public String getNumberOfSteps() {
        return numberOfSteps;
    }

    public void setNumberOfSteps(String numberOfSteps) {
        this.numberOfSteps = numberOfSteps;
    }

    public int getVerdict() {
        return verdict;
    }

    public String getVerdictIcon() {
        switch (verdict) {
            case VERDICT_SUCCESS:
                return "fa-thumbs-up";

            case VERDICT_INTERRUPT_BY_USER:
                return "fa-hand-paper";

            case VERDICT_INTERRUPT_BY_SYSTEM:
                return "fa-exclamation";

            default:
                return "fa-question";
        }
    }

    public String getVerdictTooltip() {
        switch (verdict) {
            case VERDICT_SUCCESS:
                return "Succesfully executed.";

            case VERDICT_INTERRUPT_BY_USER:
                return "Execution halted by user.";

            case VERDICT_INTERRUPT_BY_SYSTEM:
                return "Execution halted due to an error.";

            default:
                return "Unknown result";
        }
    }


}
