package nl.ou.testar.temporal.behavior;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import es.upv.staq.testar.CodingManager;
import es.upv.staq.testar.StateManagementTags;
import nl.ou.testar.StateModel.Analysis.Representation.AbstractStateModel;
import nl.ou.testar.StateModel.Persistence.OrientDB.Entity.Config;
import nl.ou.testar.temporal.structure.*;
import nl.ou.testar.temporal.util.*;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.fruit.alayer.Tag;
import org.fruit.monkey.ConfigTags;
import org.fruit.monkey.Settings;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.fruit.monkey.ConfigTags.AbstractStateAttributes;


public class TemporalController {

    // orient db instance that will create database sessions
    private OrientDB orientDB;
    private Config dbConfig;
    private String ApplicationName;
    private String ApplicationVersion;
    private String Modelidentifier;


    private String outputDir;
    private boolean ltlSPOTToWSLPath;
    private String ltlSPOTMCCommand;
    private boolean ltlSPOTEnabled;
    private boolean ctlITSToWSLPath;
    private String ctlITSMCCommand;
    private boolean ctlITSEnabled;
    private boolean ltlITSToWSLPath;
    private String ltlITSMCCommand;
    private boolean ltlITSEnabled;
    private boolean ltlLTSMINToWSLPath;
    private String ltlLTSMINMCCommand;
    private boolean ltlLTSMINEnabled;
    private String APSelectorFile;
    private String oracleFile;
    private boolean verbose;
    private boolean counterExamples;

    private boolean instrumentDeadlockState;

    private ODatabaseSession db;
    private APSelectorManager apSelectorManager;
    private TemporalModel tModel;
    private TemporalDBHelper tDBHelper;
    private List<TemporalOracle> oracleColl;

    public TemporalController(final Settings settings, String outputDir) {
        this.ApplicationName = settings.get(ConfigTags.ApplicationName);
        this.ApplicationVersion = settings.get(ConfigTags.ApplicationVersion);
        setModelidentifier(settings);
        dbConfig = makeConfig(settings);
        if (outputDir.equals("")) {
            this.outputDir = makeOutputDir(settings);
        } else {
            this.outputDir = outputDir;
        }
        tDBHelper = new TemporalDBHelper(settings);
        tModel = new TemporalModel();
        ltlSPOTToWSLPath = settings.get(ConfigTags.TemporalLTL_SPOTCheckerWSL);
        ltlSPOTMCCommand = settings.get(ConfigTags.TemporalLTL_SPOTChecker);
        ltlSPOTEnabled = settings.get(ConfigTags.TemporalLTL_SPOTChecker_Enabled);

        ctlITSToWSLPath = settings.get(ConfigTags.TemporalCTL_ITSCheckerWSL);
        ctlITSMCCommand = settings.get(ConfigTags.TemporalCTL_ITSChecker);
        ctlITSEnabled = settings.get(ConfigTags.TemporalCTL_ITSChecker_Enabled);

        ltlITSToWSLPath = settings.get(ConfigTags.TemporalLTL_ITSCheckerWSL);
        ltlITSMCCommand = settings.get(ConfigTags.TemporalLTL_ITSChecker);
        ltlITSEnabled = settings.get(ConfigTags.TemporalLTL_ITSChecker_Enabled);

        ltlLTSMINToWSLPath = settings.get(ConfigTags.TemporalLTL_LTSMINCheckerWSL);
        ltlLTSMINMCCommand = settings.get(ConfigTags.TemporalLTL_LTSMINChecker);
        ltlLTSMINEnabled = settings.get(ConfigTags.TemporalLTL_LTSMINChecker_Enabled);

        APSelectorFile = settings.get(ConfigTags.TemporalAPSelectorManager);
        oracleFile = settings.get(ConfigTags.TemporalOracles);
        verbose = settings.get(ConfigTags.TemporalVerbose);
        counterExamples = settings.get(ConfigTags.TemporalCounterExamples);
        instrumentDeadlockState = settings.get(ConfigTags.TemporalInstrumentDeadlockState);

        setDefaultAPSelectormanager();

    }

    public TemporalController(final Settings settings) {
        this(settings, "");
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setTemporalModelMetaData(AbstractStateModel abstractStateModel) {
        if (abstractStateModel != null) {
            tModel.setApplicationName(abstractStateModel.getApplicationName());
            tModel.setApplicationVersion(abstractStateModel.getApplicationVersion());
            tModel.setApplication_ModelIdentifier(abstractStateModel.getModelIdentifier());
            tModel.setApplication_AbstractionAttributes(abstractStateModel.getAbstractionAttributes());
        }


    }

    private void setModelidentifier(Settings settings) {

        //assumption is that the model is created with the same abstraction as the abstract layer.
        // we can inspect the graphmodel for the abstract layer,
        // but we cannot inspect the graphmodel for the abstraction that used on the concretelayer.
        // for new models we enforce this by setting "TemporalConcreteEqualsAbstract = true" in the test.settings file
        // copied from Main.initcodingmanager
        if (!settings.get(ConfigTags.AbstractStateAttributes).isEmpty()) {
            Tag<?>[] abstractTags = settings.get(AbstractStateAttributes).stream().map(StateManagementTags::getTagFromSettingsString).filter(tag -> tag != null).toArray(Tag<?>[]::new);
            CodingManager.setCustomTagsForAbstractId(abstractTags);
        }
        //copied from StateModelManagerFactory
        // get the abstraction level identifier that uniquely identifies the state model we are testing against.
        this.Modelidentifier = CodingManager.getAbstractStateModelHash(ApplicationName, ApplicationVersion);

    }

    private Config makeConfig(final Settings settings) {
        // used here, but controlled on StateModelPanel

        String dataStoreText;
        String dataStoreServerDNS;
        String dataStoreDirectory;
        String dataStoreDBText;
        String dataStoreUser;
        String dataStorePassword;
        String dataStoreType;
        dataStoreText = settings.get(ConfigTags.DataStore); //assume orientdb
        dataStoreServerDNS = settings.get(ConfigTags.DataStoreServer);
        dataStoreDirectory = settings.get(ConfigTags.DataStoreDirectory);
        dataStoreDBText = settings.get(ConfigTags.DataStoreDB);
        dataStoreUser = settings.get(ConfigTags.DataStoreUser);
        dataStorePassword = settings.get(ConfigTags.DataStorePassword);
        dataStoreType = settings.get(ConfigTags.DataStoreType);
        Config dbconfig = new Config();
        dbconfig.setConnectionType(dataStoreType);
        dbconfig.setServer(dataStoreServerDNS);
        dbconfig.setDatabase(dataStoreDBText);
        dbconfig.setUser(dataStoreUser);
        dbconfig.setPassword(dataStorePassword);
        dbconfig.setDatabaseDirectory(dataStoreDirectory);
        return dbconfig;
    }

    private String makeOutputDir(final Settings settings) {
        String outputDir = settings.get(ConfigTags.OutputDir);
        // check if the output directory has a trailing line separator
        if (!outputDir.substring(outputDir.length() - 1).equals(File.separator)) {
            outputDir += File.separator;
        }
        outputDir = outputDir + settings.get(ConfigTags.TemporalDirectory);

        if (settings.get(ConfigTags.TemporalSubDirectories)) {
            String runFolder = Helper.CurrentDateToFolder();
            outputDir = outputDir + File.separator + runFolder;
        }
        new File(outputDir).mkdirs();
        outputDir = outputDir + File.separator;
        return outputDir;
    }


    public TemporalModel gettModel() {
        return tModel;
    }


    public void saveAPSelectorManager(String filename) {
        JSONHandler.save(apSelectorManager, outputDir + filename, true);
    }

    public void loadApSelectorManager(String filename) {
        this.apSelectorManager = (APSelectorManager) JSONHandler.load(filename, apSelectorManager.getClass());
        apSelectorManager.updateAPKey(tModel.getApplication_BackendAbstractionAttributes());
        tDBHelper.setApSelectorManager(apSelectorManager);
    }

    public List<TemporalOracle> getOracleColl() {
        return oracleColl;
    }

    public void setOracleColl(List<TemporalOracle> oracleColl) {
        this.oracleColl = oracleColl;
        this.oracleColl.sort(Comparator.comparing(TemporalOracle::getPatternTemporalType)); //sort by type
    }

    public void updateOracleCollMetaData(boolean onlyModifiedDate) {
        LocalDateTime localDateTime = LocalDateTime.now();
        for (TemporalOracle ora : oracleColl
        ) {
            if (!onlyModifiedDate) {
                ora.setApplicationName(tModel.getApplicationName());
                ora.setApplicationVersion(tModel.getApplicationVersion());
                ora.setApplication_ModelIdentifier(tModel.getApplication_ModelIdentifier());
                ora.setApplication_AbstractionAttributes(tModel.getApplication_AbstractionAttributes());
            }
            ora.set_modifieddate(localDateTime.toString());
        }
    }

    public void setDefaultAPSelectormanager() {
        List<String> APKey = new ArrayList<>();
        if (tModel != null) {
            APKey = tModel.getApplication_BackendAbstractionAttributes();
        }
        if (APKey != null && !APKey.isEmpty()) {
            this.apSelectorManager = new APSelectorManager(true, APKey);
        } else {
            this.apSelectorManager = new APSelectorManager(true);
        }
        tDBHelper.setApSelectorManager(apSelectorManager);
    }


    // @TODO: 2019-12-29 refactor db operations to dbhelper
    private void dbClose() {
        tDBHelper.dbClose();
    }

    private void dbReopen() {
        tDBHelper.dbReopen();
    }


    public String pingDB() {
        tDBHelper.dbReopen();
        StringBuilder sb = new StringBuilder();
        List<AbstractStateModel> models = tDBHelper.fetchAbstractModels();
        if (models.isEmpty()) {
            sb.append("model count: 0\n");
        } else {
            sb.append("model count: " + models.size() + "\n");
            sb.append("Model info:\n");
            for (AbstractStateModel abs : models
            ) {
                sb.append("APP: " + abs.getApplicationName() + ", VERSION: " + abs.getApplicationVersion() + ", ID: " + abs.getModelIdentifier() + ", ABSTRACTION: " + abs.getAbstractionAttributes() + "\n");
            }
        }
        tDBHelper.dbClose();
        String dbfilename = outputDir + "Databasemodels.txt";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dbfilename))) {
            bw.write(sb.toString());
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "inspect file: " + dbfilename;
    }


    //*********************************
    private void computeTemporalModel(AbstractStateModel abstractStateModel, boolean instrumentDeadState) {

        OResultSet resultSet = tDBHelper.getConcreteStatesFromOrientDb(abstractStateModel);

        if (abstractStateModel != null) {


            //Set selectedAttibutes = apSelectorManager.getSelectedSanitizedAttributeNames();
            boolean firstDeadState = true;
            StateEncoding deadStateEnc;
            while (resultSet.hasNext()) {
                OResult result = resultSet.next();
                // we're expecting a vertex
                if (result.isVertex()) {

                    Optional<OVertex> op = result.getVertex();
                    if (!op.isPresent()) continue;

                    OVertex stateVertex = op.get();
                    StateEncoding senc = new StateEncoding(stateVertex.getIdentity().toString());
                    Set<String> propositions = new LinkedHashSet<>();
                    boolean deadstate = false;
                    Iterable<OEdge> outedges = stateVertex.getEdges(ODirection.OUT, "ConcreteAction"); //could be a SQL- like query as well
                    Iterator<OEdge> edgeiter = outedges.iterator();
                    deadstate = !edgeiter.hasNext();

                    if (deadstate) {
                        tModel.addLog("State: " + stateVertex.getIdentity().toString() + " has no outgoing transition. \n");
                        if (instrumentDeadState && firstDeadState) {
                            //add stateenc for 'Dead', inclusive dead transition selfloop;
                            deadStateEnc = new StateEncoding("#" + TemporalModel.getDeadProposition());
                            Set<String> deadStatePropositions = new LinkedHashSet<>();
                            //deadStatePropositions.add("dead");   //redundant on transitionbased automatons
                            deadStateEnc.setStateAPs(deadStatePropositions);

                            TransitionEncoding deadTrenc = new TransitionEncoding();
                            deadTrenc.setTransition(TemporalModel.getDeadProposition() + "_selfloop");
                            deadTrenc.setTargetState("#" + TemporalModel.getDeadProposition());
                            Set<String> deadTransitionPropositions = new LinkedHashSet<>();
                            deadTransitionPropositions.add(TemporalModel.getDeadProposition());
                            deadTrenc.setTransitionAPs(deadTransitionPropositions);
                            List<TransitionEncoding> deadTrencList = new ArrayList<>();
                            deadTrencList.add(deadTrenc);
                            deadStateEnc.setTransitionColl(deadTrencList);
                            tModel.addStateEncoding(deadStateEnc, false);
                            firstDeadState = false;
                        }
                        if (!instrumentDeadState)
                            stateVertex.setProperty(TagBean.IsDeadState.name(), true);  //candidate for refactoring
                    }
                    for (String propertyName : stateVertex.getPropertyNames()) {
                        tDBHelper.computeProps(propertyName, stateVertex, propositions, null, false, false);
                    }
                    propositions.addAll(tDBHelper.getWidgetPropositions(senc.getState(), tModel.getApplication_BackendAbstractionAttributes()));// concrete widgets
                    senc.setStateAPs(propositions);
                    if (instrumentDeadState && deadstate) {
                        TransitionEncoding deadTrenc = new TransitionEncoding();
                        deadTrenc.setTransition("#" + TemporalModel.getDeadProposition() + "_" + stateVertex.getIdentity().toString());
                        deadTrenc.setTargetState("#" + TemporalModel.getDeadProposition());
                        Set<String> deadTransitionPropositions = new LinkedHashSet<>();
                        deadTransitionPropositions.add(TemporalModel.getDeadProposition());
                        deadTrenc.setTransitionAPs(deadTransitionPropositions);
                        List<TransitionEncoding> deadTrencList = new ArrayList<>();
                        deadTrencList.add(deadTrenc);
                        senc.setTransitionColl(deadTrencList);
                    } else senc.setTransitionColl(tDBHelper.getTransitions(senc.getState()));

                    tModel.addStateEncoding(senc, false);
                }
            }
            resultSet.close();
            tModel.finalizeTransitions(); //update once. this is a costly operation
            for (StateEncoding stenc : tModel.getStateEncodings()
            ) {
                List<String> encodedConjuncts = new ArrayList<>();
                for (TransitionEncoding tren : stenc.getTransitionColl()
                ) {
                    String enc = tren.getEncodedTransitionAPConjunct();
                    if (encodedConjuncts.contains(enc)) {
                        tModel.addLog("State: " + stenc.getState() + " has  non-deterministic transition: " + tren.getTransition());
                    } else encodedConjuncts.add(enc);
                }
            }
            tModel.setTraces(tDBHelper.fetchTraces(tModel.getApplication_ModelIdentifier()));
            List<String> initStates = new ArrayList<>();
            for (TemporalTrace trace : tModel.getTraces()
            ) {
                TemporalTraceEvent traceevent = trace.getTraceEvents().get(0);
                initStates.add(traceevent.getState());
            }
            tModel.setInitialStates(initStates);
            tModel.setAPSeparator(apSelectorManager.getApEncodingSeparator());

            for (String ap : tModel.getModelAPs()    // check the resulting model for DeadStates
            ) {
                if (ap.contains(apSelectorManager.getApEncodingSeparator() + TagBean.IsDeadState.name())) {
                    tModel.addLog("WARNING: Model contains dead states (there are states without outgoing edges)");
                    break;
                }
            }

        }
    }


    private AbstractStateModel getAbstractStateModel() {
        AbstractStateModel abstractStateModel;
        abstractStateModel = tDBHelper.selectAbstractStateModelByModelId(Modelidentifier);
        if (abstractStateModel == null) {
            tModel.addLog("ERROR: Model with identifier : " + Modelidentifier + " was not found in the graph database <" + dbConfig.getDatabase()+">");
            System.out.println("ERROR: Model with identifier : " + Modelidentifier + " was not found in the graph database <" + dbConfig.getDatabase()+">");
        }
        return abstractStateModel;
    }


    public boolean saveToGraphMLFile(String file, boolean excludeWidget) {
        AbstractStateModel abstractStateModel = getAbstractStateModel();
        if (abstractStateModel != null) {
            return tDBHelper.saveToGraphMLFile(abstractStateModel, outputDir + file, excludeWidget);
        } else return false;
    }

    private void saveModelAsJSON(String toFile) {
        JSONHandler.save(tModel, outputDir + toFile);
    }

    public boolean saveModelForChecker(TemporalType tmptype, String file) {
        boolean b = false;
        String contents = "";
        String contents1 = "";

        if (tmptype.equals(TemporalType.LTL) || tmptype.equals(TemporalType.LTL_SPOT)) {
            contents = tModel.makeHOAOutput();
            b = true;
        }
        if (tmptype.equals(TemporalType.CTL) || tmptype.equals(TemporalType.LTL_ITS) || tmptype.equals(TemporalType.LTL_LTSMIN)) {

//            **under construction
//            contents1 =tModel.makeGALOutput();
//            File output = new File(file+".gal");
//            saveStringToFile(contents1,output);
            contents = tModel.makeETFOutput();
            b = true;
        }
        if (b) {
            File output = new File(file);
            saveStringToFile(contents,output);
        }
        return b;
    }


    public void saveFormulaFiles(List<TemporalOracle> oracleColl, File output) {
        saveFormulaFiles(oracleColl, output, true);
    }

    public void saveFormulaFiles(List<TemporalOracle> oracleColl, File output, boolean doTransformation) {

        String contents = tModel.validateAndMakeFormulas(oracleColl, doTransformation);
        TemporalController.saveStringToFile(contents, output);
    }

    public static void saveStringToFile(String contents, File output) {

        try {

            if (output.exists() || output.createNewFile()) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output.getAbsolutePath()), StandardCharsets.UTF_8));
                writer.append(contents);
                writer.close();
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }


    public void MCheck() {

        MCheck(APSelectorFile, oracleFile, verbose, counterExamples, instrumentDeadlockState,
                ltlSPOTMCCommand, ltlSPOTToWSLPath, ltlSPOTEnabled,
                ctlITSMCCommand, ctlITSToWSLPath, ctlITSEnabled,
                ltlITSMCCommand, ltlITSToWSLPath, ltlITSEnabled,
                ltlLTSMINMCCommand, ltlLTSMINToWSLPath, ltlLTSMINEnabled);
    }


    public void MCheck(String APSelectorFile, String oracleFile,
                       boolean verbose, boolean counterExamples, boolean instrumentDeadState,
                       String ltlSpotMCCommand, boolean ltlSpotWSLPath, boolean ltlSpotEnabled,
                       String ctlItsMCCommand,  boolean ctlItsWSLPath, boolean ctlItsEnabled,
                       String ltlItsMCCommand, boolean ltlItsWSLPath, boolean ltlItsEnabled,
                       String ltlLtsminMCCommand, boolean ltlLtsminWSLPath, boolean ltlltsminEnabled) {
        try {

            System.out.println(LocalTime.now() + " | " + "Temporal model-checking started");
            List<TemporalOracle> fromcoll = CSVHandler.load(oracleFile, TemporalOracle.class);
            if (fromcoll == null) {
                System.err.println("Error: verify the file at location '" + oracleFile + "'");
            } else {
                tModel = new TemporalModel();
                AbstractStateModel abstractStateModel = getAbstractStateModel();
                if (abstractStateModel == null){
                    System.err.println("Error: StateModel not available");
                }
                else {

                setTemporalModelMetaData(abstractStateModel);
                loadApSelectorManager(APSelectorFile);
                String APCopy = "copy_of_used_" + Paths.get(APSelectorFile).getFileName().toString();
                String OracleCopy = "copy_of_used_" + Paths.get(oracleFile).getFileName().toString();
                if (verbose) {
                    Files.copy((new File(APSelectorFile).toPath()),
                            new File(outputDir + APCopy).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Files.copy((new File(oracleFile).toPath()),
                            new File(outputDir + OracleCopy).toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                setOracleColl(fromcoll);
                updateOracleCollMetaData(false);

                String strippedFile;
                String filename = Paths.get(oracleFile).getFileName().toString();
                if (filename.contains(".")) strippedFile = filename.substring(0, filename.lastIndexOf("."));
                else strippedFile = filename;
                File inputvalidatedFile = new File(outputDir + strippedFile + "_inputvalidation.csv");
                File modelCheckedFile = new File(outputDir + strippedFile + "_modelchecked.csv");

                Map<TemporalType, List<TemporalOracle>> oracleTypedMap =
                        fromcoll.stream().collect(Collectors.groupingBy(TemporalOracle::getPatternTemporalType));

                makeTemporalModel(APSelectorFile, verbose, instrumentDeadState);
                if (verbose) {
                    System.out.println(LocalTime.now() + " | " + "exporting model files");
                    saveToGraphMLFile("GraphML.XML", false);
                    saveToGraphMLFile("GraphML_NoWidgets.XML", true);
                    saveModelAsJSON("APEncodedModel.json");
                }
                List<TemporalOracle> initialoraclelist = new ArrayList<>();
                List<TemporalOracle> finaloraclelist = new ArrayList<>();

                for (Map.Entry<TemporalType, List<TemporalOracle>> oracleentry : oracleTypedMap.entrySet()
                ) {
                    List<TemporalOracle> modelCheckedOracles = null;
                    File automatonFile = null;
                    String oracleType = oracleentry.getKey().name();
                    List<TemporalOracle> oracleList = oracleentry.getValue();

                    File formulaFile = new File(outputDir + oracleType + "_formulas.txt");
                    File resultsFile = new File(outputDir + oracleType + "_results.txt");
                    File syntaxformulaFile = new File(outputDir + oracleType + "_syntaxcheckedformulas.txt");
                    File convertedformulaFile = new File(outputDir + oracleType + "_convertedformulas.txt");

                    initialoraclelist.addAll(oracleList);
                    System.out.println(LocalTime.now() + " | " + oracleType + " invoking the " + "backend model-checker");
                    if (ltlSpotEnabled && (TemporalType.valueOf(oracleType) == TemporalType.LTL || TemporalType.valueOf(oracleType) == TemporalType.LTL_SPOT)) {
                        automatonFile = new File(outputDir + "Model.hoa");
                        saveModelForChecker(TemporalType.valueOf(oracleType), automatonFile.getAbsolutePath());
                        String aliveprop = gettModel().getPropositionIndex("!" + TemporalModel.getDeadProposition()); //instrumentDeadState will determine whether this return value is ""
                        saveFormulaFiles(oracleList, formulaFile);
                        Helper.LTLMC_BySPOT(ltlSpotMCCommand, ltlSpotWSLPath, counterExamples, automatonFile.getAbsolutePath(),
                                formulaFile.getAbsolutePath(), aliveprop, resultsFile.getAbsolutePath());
                        ResultsParser sParse = new SPOT_LTL_ResultsParser();//decode results
                        sParse.setTmodel(gettModel());
                        sParse.setOracleColl(oracleList);
                        System.out.println(LocalTime.now() + " | " + oracleType + " verifying the results form the backend model-checker");
                        modelCheckedOracles = sParse.parse(resultsFile);
                        if (modelCheckedOracles == null) {
                            System.err.println(LocalTime.now() + " | " + oracleType + "  ** Error: no results from the model-checker");
                        }
                    } else if ((ltlItsEnabled && TemporalType.valueOf(oracleType) == TemporalType.LTL_ITS) ||
                            (ltlltsminEnabled && TemporalType.valueOf(oracleType) == TemporalType.LTL_LTSMIN)) {
                        automatonFile = new File(outputDir + "Model.etf");
                        saveModelForChecker(TemporalType.valueOf(oracleType), automatonFile.getAbsolutePath());

                        //formula ltl model variant converter
                        // instrumentDeadState will determine whether this return value is ""
                        String aliveprop = gettModel().getPropositionIndex("!" + TemporalModel.getDeadProposition());
                        if (!aliveprop.equals("")) {
                            saveFormulaFiles(oracleList, formulaFile, false);

                            Helper.LTLVerifyFormula_BySPOT(ltlSpotMCCommand, ltlSpotWSLPath, formulaFile.getAbsolutePath(), syntaxformulaFile.getAbsolutePath());
                            List<String> tmpformulas = SPOT_LTLFormula_ResultsParser.parse(syntaxformulaFile, true);
                            List<TemporalOracle> tmporacleList = new ArrayList<>();
                            int j = 0;
                            for (TemporalOracle ora : oracleList
                            ) {
                                TemporalOracle oraClone = ora.clone();
                                TemporalPatternBase pat = oraClone.getPatternBase();
                                pat.setPattern_Formula(tmpformulas.get(j));
                                //oraClone.setPatternBase(pat);
                                tmporacleList.add(oraClone);
                                j++;
                            }
                            saveFormulaFiles(tmporacleList, formulaFile, true);
                        } else {
                            saveFormulaFiles(oracleList, formulaFile, true);
                        }
                        ResultsParser sParse = null;
                        if (ltlItsEnabled && TemporalType.valueOf(oracleType) == TemporalType.LTL_ITS) {
                            Helper.LTLMC_ByITS(ltlItsMCCommand, ltlItsWSLPath, counterExamples, automatonFile.getAbsolutePath(),
                                    formulaFile.getAbsolutePath(), resultsFile.getAbsolutePath());
                            sParse = new ITS_LTL_ResultsParser();//decode results
                        } else {
                            Helper.LTLMC_ByLTSMIN(ltlLtsminMCCommand, ltlLtsminWSLPath, counterExamples,
                                    automatonFile.getAbsolutePath(), formulaFile.getAbsolutePath(), resultsFile.getAbsolutePath());
                            sParse = new LTSMIN_LTL_ResultsParser();//decode results

                        }

                        sParse.setTmodel(gettModel());
                        sParse.setOracleColl(oracleList);
                        System.out.println(LocalTime.now() + " | " + oracleType + " verifying the results form the backend model-checker");
                        modelCheckedOracles = sParse.parse(resultsFile);
                        if (modelCheckedOracles == null) {
                            System.err.println(LocalTime.now() + " | " + oracleType + "  ** Error: no results from the model-checker");
                        }
                    } else if (ctlItsEnabled &&  (TemporalType.valueOf(oracleType) == TemporalType.CTL ||
                                                TemporalType.valueOf(oracleType) == TemporalType.CTL_ITS)) {
                        automatonFile = new File(outputDir + "Model.etf");
                        saveModelForChecker(TemporalType.valueOf(oracleType), automatonFile.getAbsolutePath());
                        //ITS-CTL checker: not using witness because this is  difficult to understand and to parse and show.
                        //LTSMIN-CTL bug: gives a segmentation fault when checking ctl, but same model can be checked on ltl . :-)
                        saveFormulaFiles(oracleList, formulaFile);
                        Helper.CTLMC_ByITS(ctlItsMCCommand, ctlItsWSLPath, counterExamples, automatonFile.getAbsolutePath(),
                                formulaFile.getAbsolutePath(), resultsFile.getAbsolutePath());
                        ResultsParser sParse = new ITS_CTL_ResultsParser();//decode results
                        sParse.setTmodel(gettModel());
                        sParse.setOracleColl(oracleList);
                        System.out.println(LocalTime.now() + " | " + oracleType + " verifying the results form the backend model-checker");
                        modelCheckedOracles = sParse.parse(resultsFile);
                        if (modelCheckedOracles == null) {
                            System.err.println(LocalTime.now() + " | " + oracleType + "  ** Error: no results from the model-checker");
                        }
                    } else {
                        System.err.println(LocalTime.now() + " | " + oracleType + " Warning:  this oracle type is not implemented or disabled");
                    }
                    if (modelCheckedOracles != null) {
                        finaloraclelist.addAll(modelCheckedOracles);
                    }


                    if (!verbose) {
                        Files.delete(automatonFile.toPath());
                        Files.delete(resultsFile.toPath());
                        Files.delete(formulaFile.toPath());
                        Files.delete(syntaxformulaFile.toPath());
                        Files.delete(convertedformulaFile.toPath());
                        Files.delete(inputvalidatedFile.toPath());
                    }
                    System.out.println(LocalTime.now() + " | " + oracleType + " model-checking completed");
                }
                CSVHandler.save(initialoraclelist, inputvalidatedFile.getAbsolutePath());
                if (finaloraclelist.size() != fromcoll.size()) {
                    System.err.println(LocalTime.now() + " | " + "** Warning: less oracle verdicts received than in original collection");
                    System.err.println(LocalTime.now() + " | " + "from file: "+ Paths.get(oracleFile).getFileName());
                }
                CSVHandler.save(finaloraclelist, modelCheckedFile.getAbsolutePath());
            }
            }
            System.out.println(LocalTime.now() + " | " + "Temporal model-checking completed");
        } catch (Exception f) {
            f.printStackTrace();
        }
    }

    public void makeTemporalModel(String APSelectorFile, boolean verbose, boolean instrumentDeadState) {
        try {
            System.out.println(LocalTime.now() + " | " + "compute temporal model started");

            AbstractStateModel abstractStateModel = getAbstractStateModel();
            if (abstractStateModel == null){
                System.err.println("Error: StateModel not available");
            }
            else{
            setTemporalModelMetaData(abstractStateModel);
            if (APSelectorFile.equals("")) {
                setDefaultAPSelectormanager();
                saveAPSelectorManager("default_APSelectorManager.json");
            }
            tDBHelper.dbReopen();
            computeTemporalModel(abstractStateModel, instrumentDeadState);
            tDBHelper.dbClose();
            if (verbose) {
                saveModelAsJSON("APEncodedModel.json");
            }

            System.out.println(LocalTime.now() + " | " + "compute temporal model completed");
            }
        } catch (Exception f) {
            f.printStackTrace();
        }

    }

    public void generateOraclesFromPatterns(String APSelectorfile, String patternFile, String patternConstraintFile, int tactic_oraclesPerPattern) {
        try {
            System.out.println(" potential Oracle generator started \n");
            makeTemporalModel(APSelectorfile, false, true);
            List<TemporalPattern> patterns = CSVHandler.load(patternFile, TemporalPattern.class);
            List<TemporalPatternConstraint> patternConstraints = null;
            if (!patternConstraintFile.equals("")) {
                patternConstraints = CSVHandler.load(patternConstraintFile, TemporalPatternConstraint.class);
            }

            File PotentialoracleFile = new File(outputDir + "TemporalPotentialOracles.csv");

            List<TemporalOracle> fromcoll;
            fromcoll = generatePotentialOracles(patterns, patternConstraints, tactic_oraclesPerPattern);
            CSVHandler.save(fromcoll, PotentialoracleFile.getAbsolutePath());

            System.out.println(" potential Oracle generator completed \n");
        } catch (Exception f) {
            f.printStackTrace();
        }

    }


    public List<TemporalOracle> generatePotentialOracles(List<TemporalPattern> patterns, List<TemporalPatternConstraint> patternConstraints, int tactic_oraclesPerPattern) {
        // there is no check on duplicate assignments:  a pattern can turn up as a oracle with exactly the same assignments.
        // the risk is remote due to the randomness on AP selection and e=randomness on constraint-set selection.
        List<TemporalOracle> potentialOracleColl = new ArrayList<>();
        List<String> modelAPSet = new ArrayList<>(tModel.getModelAPs());
        int trylimitConstraint = Math.min(250, 2 * modelAPSet.size());
        Random APRnd = new Random(5000000);
        for (TemporalPattern pat : patterns
        ) {
            Map<String, String> ParamSubstitutions;
            TemporalPatternConstraint patternConstraint = null;
            int patcIndex;
            TreeMap<Integer, Map<String, String>> constrainSets = null;
            boolean passConstraint = false;
            Random constraintRnd = new Random(6000000);
            int cSetindex = -1;
            Map<String, String> constraintSet = null;
            patcIndex = -1;
            if (patternConstraints != null) {
                for (int h = 0; h < patternConstraints.size(); h++) {
                    patternConstraint = patternConstraints.get(h);
                    if (pat.getPattern_Formula().equals(patternConstraint.getPattern_Formula())) {
                        patcIndex = h;
                        break;
                    }
                }
            }
            if (patcIndex != -1) {
                constrainSets = patternConstraint.getConstraintSets();
            }
            for (int i = 0; i < tactic_oraclesPerPattern; i++) {
                TemporalOracle potentialOracle = new TemporalOracle();
                if (constrainSets != null) {
                    cSetindex = constraintRnd.nextInt(constrainSets.size());//start set. constrainset number is 1,2,3,...
                }
                ParamSubstitutions = new HashMap<>();
                for (String param : pat.getPattern_Parameters()
                ) {
                    passConstraint = false;
                    String provisionalParamSubstitution = null;
                    if (constrainSets == null) {
                        provisionalParamSubstitution = modelAPSet.get(APRnd.nextInt(modelAPSet.size() - 1));
                        ParamSubstitutions.put(param, provisionalParamSubstitution);
                        passConstraint = true;  //virtually true
                    } else {
                        for (int k = 1; k < constrainSets.size() + 1; k++) {//constrainset number is 1,2,3,...
                            int ind = (k + cSetindex) % (constrainSets.size() + 1);
                            constraintSet = constrainSets.get(ind);
                            if (constraintSet.containsKey(param)) {
                                Pattern regexPattern = CachedRegexPatterns.addAndGet(constraintSet.get(param));
                                if (regexPattern == null) {
                                    continue; //no pass for this constraint-set due to invalid pattern
                                } else {
                                    for (int j = 0; j < trylimitConstraint; j++) {
                                        provisionalParamSubstitution = modelAPSet.get(APRnd.nextInt(modelAPSet.size() - 1));
                                        Matcher m = regexPattern.matcher(provisionalParamSubstitution);
                                        if (m.matches()) {
                                            ParamSubstitutions.put(param, provisionalParamSubstitution);
                                            passConstraint = true;
                                            break;// go to next parameter
                                        }
                                    }
                                }
                            } else {
                                provisionalParamSubstitution = modelAPSet.get(APRnd.nextInt(modelAPSet.size() - 1));
                                ParamSubstitutions.put(param, provisionalParamSubstitution);
                                passConstraint = true;  //virtually true
                                break;// go to next parameter
                            }
                            if (passConstraint) {
                                break;
                            }
                        }
                    }
                }
                potentialOracle.setPatternBase(pat); //downcasting of pat
                potentialOracle.setApplicationName(tModel.getApplicationName());
                potentialOracle.setApplicationVersion(tModel.getApplicationVersion());
                potentialOracle.setApplication_AbstractionAttributes(tModel.getApplication_AbstractionAttributes());
                potentialOracle.setApplication_ModelIdentifier(tModel.getApplication_ModelIdentifier());
                if (passConstraint) { //assignment found, save and go to next round for a pattern
                    if (cSetindex != -1) {
                        potentialOracle.setPattern_ConstraintSet(cSetindex + 1);// sets numbers from 1,2,3,...
                    }
                    MultiValuedMap<String, String> pattern_Substitutions = new HashSetValuedHashMap<>();
                    for (Map.Entry<String, String> paramsubst : ParamSubstitutions.entrySet()
                    ) {
                        pattern_Substitutions.put("PATTERN_SUBSTITUTION_" + paramsubst.getKey(), paramsubst.getValue());// improve?
                    }
                    potentialOracle.setPattern_Substitutions(pattern_Substitutions);
                    potentialOracle.setOracle_validationstatus(ValStatus.CANDIDATE);
                } else {
                    // no assignment found
                    potentialOracle.setOracle_validationstatus(ValStatus.ERROR);
                    potentialOracle.addLog("No valid assignment of substitutions found. Advise: review ConstraintSets");
                }
                potentialOracleColl.add(potentialOracle);
            }
        }
        return potentialOracleColl;
    }
}


