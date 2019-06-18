package nl.ou.testar.temporal.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//@JsonRootName(value="TemporalProperties")
public class TemporalModel extends TemporalBean{

    private List<StateEncoding> stateEncodings; //Integer:  to concretstateID
    private List<TemporalTrace> traces; //
    private List<String> modelAPs; //AP<digits> to widget property map:
    private String formatVersion="20190603";



    public TemporalModel(String applicationName, String applicationVersion, String modelIdentifier, Set abstractionAttributes) {
        super(applicationName, applicationVersion, modelIdentifier, abstractionAttributes);
        this.stateEncodings = new ArrayList<>();
        this.modelAPs = new ArrayList<String>();
    }



    public List<String> getModelAPs() {
        return modelAPs;
    }

    public void setModelAPs(List<String> modelAPs) {
        this.modelAPs = modelAPs;
    }

    public List<TemporalTrace> getTraces() {
        return traces;
    }

    public void setTraces(List<TemporalTrace> stateTransistionSequence) {
        this.traces = stateTransistionSequence;
    }

    public List<StateEncoding> getStateEncodings() {
        return stateEncodings;
    }

    public void setStateEncodings(List<StateEncoding> stateEncodings) {

        this.stateEncodings = stateEncodings;
        for (StateEncoding stateEnc: stateEncodings) {
            this.modelAPs.addAll(stateEnc.getStateAPs());
        }
        for (StateEncoding stateEnc: stateEncodings) {  // observer pattern?
            stateEnc.updateAllTransitionConjuncts(modelAPs);
        }
    }

    public String getFormatVersion() {        return formatVersion;   }

    public void setFormatVersion(String formatVersion) {    this.formatVersion = formatVersion;    }


    //custom
    public void addStateEncoding(StateEncoding stateEncoding) {

        stateEncodings.add(stateEncoding);
        this.modelAPs.addAll(stateEncoding.getStateAPs());
        for (StateEncoding stateEnc: stateEncodings) {// observer pattern?
            stateEnc.updateAllTransitionConjuncts(modelAPs);
        }
    }


    public void fetchDBModel(String filter){
        //loop through model ,
        //  query db model and set in header properties
        // query concret states and moke stat encoding per state
        //===> define properties to collect strategies,
        //per state get outbound edges and make transition encodings
        //
        //
        //  collect AP's make a listentry : [Statexxx, list of AP's] apply filters
        //   state list, +ap's



    }
    private String makeHOAOutput(){
        //see http://adl.github.io/hoaf/
    StringBuilder result=new StringBuilder();
    result.append("HOA v1\n");
    result.append("States: ");
    result.append(stateEncodings.size());
    result.append("\n");
    result.append("Start: 0\n");
    result.append("Acceptance: 1 Inf(1))\n");  //==Buchi
    result.append("AP: ");
    result.append(modelAPs.size());
        int i=0;
        for (String ap: modelAPs) {
            result.append(" \"ap");
            result.append(i);
            result.append("\"");
            i++;
        }
     result.append("\n");
     result.append("--BODY--");
        int s=0;
        for (StateEncoding stateenc: stateEncodings) {
            result.append("State: ");
            result.append(s);
            result.append("\n");
            for (TransitionEncoding trans:stateenc.getTransitionColl()  ) {
                result.append(trans.getEncodedAPConjunct());
                int targetstateindex= stateEncodings.indexOf(trans.getTargetState());
                result.append(" "+targetstateindex);
                result.append(" {0}\n");  //all are in the same buchi acceptance set
            }
        }
        result.append("--END--");
        result.append("EOF_HOA");
        return result.toString();
    }



}