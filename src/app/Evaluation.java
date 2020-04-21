package app;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import classification.DiplomaticClassifier;
import classification.ProbabilisticClassifier;
import config.ClassificationConfig;
import config.Milestones;
import dataClasses.diploma.AbstractDiploma;
import dataClasses.diploma.TrainingDiploma;
import dataClasses.evaluationResults.LabelEvaluationResult;
import dataClasses.evaluationResults.MacroAverageResult;
import dataClasses.evaluationResults.MicroAverageResult;
import dataClasses.label.DiplomaticLabel;
import dataClasses.sentence.AbstractSentence;
import dataClasses.sentence.TrainingSentence;
import helpers.ReaderWriter;
import preprocessing.Preprocessor;
import preprocessing.VectorType;

/**
 * Die Klasse Evaluation bietet nur eine öffentliche Funktion an, nämlich die Funktion evaluate(). Mithilfe dieser Funktion
 * können Kreuzvalidierungen für verschiedene Konfigurationsszenarien durchgeführt werden, deren Ergebnisse dann in Text-Dateien
 * gespeichert werden. 
 * @author Alina Ostrowski
 *
 */
public class Evaluation {

	// Pfade zu den benötigten Dateien
	private String testDataPath = DiplomaAnalyzerApp.trainingPath;
	private String resolverPath = DiplomaAnalyzerApp.resolverPath;
	private String indicatorPath = DiplomaAnalyzerApp.indicatorPath;
	private String abbreviationPath = DiplomaAnalyzerApp.abbreviationPath;
	private String capitalLetterPath = DiplomaAnalyzerApp.capitalLetterPath;
	private String paranthesisPath = DiplomaAnalyzerApp.paranthesisPath;
	
	/**
	 * Ordner, in dem die Ergebnisse der Evaluation gespeichert werden sollen.
	 */
	private String evalResultPath;
	
	private List<TrainingDiploma> testData;
	private List<List<TrainingDiploma>> testGroups;
	private int groupNumber = 3;
	
	private DiplomaticClassifier dc;
	private Preprocessor pp;
	/**
	 * Set, das die verschiedenen zu testenden Konfigurationen (Evaluationsszenarien) enthält.
	 */
	private Set<ClassificationConfig> configSet;
	private List<DiplomaticLabel> labels;

	private int sentenceCount = 0;
	private Map<ClassificationConfig, List<Double>> averageResults;
	
	/**
	 * @param evalResultPath Ordner, in dem die Ergebnisse der Evaluation gespeichert werden sollen.
	 */
	public Evaluation(String evalResultPath){
		this.evalResultPath = evalResultPath;
		this.labels = Arrays.asList(DiplomaticLabel.values());
		// Initialisierung aller möglichen Evaluationsszenarien
		this.initializeConfigList(2);	
		this.pp = new Preprocessor(resolverPath, abbreviationPath, capitalLetterPath, paranthesisPath);
		this.averageResults = new HashMap<ClassificationConfig, List<Double>>();
	}
	
	
	public void evaluate(){
		
		// Initialisierung der Trainingsdaten
		System.out.println("***INITIALIZING TRAINING DIPLOMAS***");
		testData = DiplomaAnalyzerApp.initializeTrainingDiplomas(testDataPath, pp);
		
		for(TrainingDiploma dipl : testData){
			sentenceCount  += dipl.getSentCount();
		}
		
		System.out.println("Found "+testData.size()+" diplomas with "+sentenceCount+" sentences in total for evaluation.");
		System.out.println();
		
		// bei ausreichender Gruppenmenge die Trainingsdaten in Testgruppen aufteilen
		testGroups = new ArrayList<>();
		if(testData.size() < groupNumber*2){
			testGroups.add(testData);
		} else {
			createTestGroups();
		}

		Milestones defMs = new Milestones();
		dc = new DiplomaticClassifier(indicatorPath, defMs);

		// Für jedes Evaluationsszenario eine Kreuzvalidierung durchführen und die Ergebnisse in einer Text-Datei speichern
		System.out.println("***STARTING EVALUATION***");
		System.out.println();
		int configCount = configSet.size();
		int i = 1;
		for(ClassificationConfig config : configSet){
			System.out.println("CLASSIFICATION AND EVALUATION FOR CONFIGURATION "+i+" OF "+configCount);
			Integer[][] confusionmatrix = generateConfusionMatrix(config);
			
			System.out.println("EVALUATION_APP: Calculating evaluation results...");
			List<LabelEvaluationResult> labelResults = getLabelEvaluationResults(confusionmatrix);
			MacroAverageResult mar = new MacroAverageResult(labelResults);
			MicroAverageResult mir = new MicroAverageResult(labelResults);
			printEvaluationResultsToFile(config, confusionmatrix, labelResults, mar, mir);
			
			List<Double> avResults = new ArrayList<>();
			avResults.add(mar.getPrecision());
			avResults.add(mar.getRecall());
			avResults.add(mar.getAccuracy());
			avResults.add(mar.getF1Value());
			avResults.add(mir.getPrecision());
			avResults.add(mir.getAccuracy());
			averageResults.put(config, avResults);
			
			i++;
			System.out.println();
		}

		printAverageEvaluationResultsToFile();
		
		System.out.println("Finished evaluation.");
		
	}


	/**
	 * Erstellt für alle möglichen Kombinationen an Konfigurationselementen eine Konfiguration und fügt sie der configList des Objekts hinzu.
	 * @param toleranceCount Für welche Toleranz-Werte sollen Konfigurationen erstellt werden? Die Methode erstellt Konfigurationen für alle
	 * Werte bis zu dem übergebenen toleranceCount (einschließlich).
	 */
	private void initializeConfigList(int toleranceCount) {
		configSet = new HashSet<>();
		VectorType[] vectors = VectorType.values();
		for(int i = 0; i < vectors.length; i++){
			for(int t = 0; t <= toleranceCount; t++){
				ClassificationConfig confFalse = new ClassificationConfig(t, vectors[i], false);
				ClassificationConfig confTrue = new ClassificationConfig(t, vectors[i], true);
				configSet.add(confFalse);
				configSet.add(confTrue);
			}
		}
		
	}


	private void createTestGroups() {
		int groupSize = (int) Math.ceil(testData.size() / groupNumber);
		int milestone = groupSize-1;
		
		Collections.shuffle(testData);
		List<TrainingDiploma> group = new ArrayList<>();
		for(int i = 0; i < testData.size(); i++){
			
			if (i == milestone || i == testData.size()){
				testGroups.add(group);
				group = new ArrayList<>();
				milestone += groupSize;
			} else if(i < milestone){
				group.add(testData.get(i));
			} 
		}
	}

	/**
	 * Erstellt anhand der übergebenen Konfusionsmatrix für jedes mögliche Label ein
	 * LabelEvaluationResult, dass die Evaluationswerte true positives, false positives, true negatives
	 * sowie false negatives übergeben bekommt.
	 * @param cm Die Konfusionsmatrix, anhand derer die Evaluationswerte ermittelt werden. Die erste Dimension muss dabei den true Labeln, die zweite den predicted entsprechen.
	 * @return Eine Liste mit allen angelegten LabelEvaluationResult-Objekten
	 */
	private List<LabelEvaluationResult> getLabelEvaluationResults(Integer[][] cm) {

		// 1. i der cm = true label
		// 2. j der cm = predicted label
		List<LabelEvaluationResult> resultList = new ArrayList<>();
		for(int i = 0; i < labels.size(); i++){
			int tp = cm[i][i];
			int fp = 0;
			for(int j = 0; j < cm[i].length; j++){
				fp += cm[j][i]; 
			}
			fp -= tp;
			
			int fn = 0;
			for(int j = 0; j < cm[i].length; j++){
				fn += cm[i][j]; 
			}
			fn -= tp;
			
			int tn = sentenceCount - tp - fp - fn;
			
			LabelEvaluationResult result = new LabelEvaluationResult(labels.get(i), tp, fp, tn, fn);
			resultList.add(result);
		}
		
		return resultList;
	}

	/**
	 * Führt für jede Gruppe aus testGroups einen Klassifikationsdurchlauf mit den übergebenen Konfigurationen durch und gleicht die vorhergesagten Ergebnisse
	 * mit den echten Labeln ab. Die Resultate werden in einer Konfusionsmatrix gespeichert, die zurückgegeben wird.
	 * @param config Die Konfiguration, die für die Klassifizierung genutzt werden soll.
	 * @return Einen zweidimensionalen Array, der die Ergebnisse für die Kombinationen true positives, false positives, true negatives
	 * sowie false negatives enthält. Die erste Dimension entspricht dabei den true Labeln, die zweite den predicted.
	 */
	private Integer[][] generateConfusionMatrix(ClassificationConfig config) {
		
		// Konfusionsmatrix anlegen
		List<DiplomaticLabel> labels = Arrays.asList(DiplomaticLabel.values());
		Integer[][] confusionmatrix = new Integer[labels.size()][labels.size()];
		for(int i = 0; i < confusionmatrix.length; i++){
			for(int j = 0; j < confusionmatrix[i].length; j++){
				confusionmatrix[i][j] = 0;
			}
		}
		
		// Für jede testGruppe einen Klassifikationsdurchlauf machen und die Ergebnise in der Konfusionsmatrix speichern
		int groupCount = 1;
		for(List<TrainingDiploma> testGroup : testGroups){
			System.out.println("GROUP "+groupCount+" OF "+testGroups.size());
			
			// alle Sätze auf ihren Ursprungszustand vor der Präprozessierung und Klassifizierung zurücksetzen
			for(TrainingDiploma dipl : testData){
				List<TrainingSentence> sents = new ArrayList<>(dipl.getSentences());
				for(TrainingSentence sent : sents){
					sent.reset();
				}
				pp.prepareAll(new ArrayList<AbstractSentence>(sents));
			}
			List<TrainingDiploma> trainingDiplomas = new ArrayList<>(testData);
			trainingDiplomas.removeAll(testGroup);
			ProbabilisticClassifier pc = new ProbabilisticClassifier(pp, trainingDiplomas, config.getUseBigramsInsteadOfTokens(),config.getVectorType());
			
			Milestones ms = new Milestones(trainingDiplomas, config.getSequProbsTolerance());
			dc.setMilestones(ms);

			DiplomaAnalyzerApp.classify(new ArrayList<AbstractDiploma>(testGroup), dc, pc);
			
			// Ergebnisse in der Konfigurationsmatrix speichern
			for(TrainingDiploma dipl : testGroup){
				for(TrainingSentence sent : dipl.getSentences()){
					int indexOfTrueLabel = labels.indexOf(sent.getTruePartLabel());
					int indexOfPredictedLabel = labels.indexOf(sent.getLabel());
					
					Integer count = confusionmatrix [indexOfTrueLabel][indexOfPredictedLabel];
					count ++;
					confusionmatrix [indexOfTrueLabel][indexOfPredictedLabel] = count;	
				}
			}
			groupCount++;
		}
		
		return confusionmatrix;
	}

	/**
	 * Erzeugt eine Text-Respräsentation der Evaluationsergebnisse für die übergebene Konfiguration und speichert diese in einer
	 * Textdatei durch den evalResultPath definierten Ordner.
	 * @param config Die Konfiguration, für die die Ergebnisse gelten.
	 * @param cm Die Konfusionsmatrix der Kreuzvalididerung für diese Konfiguration.
	 * @param labelResults Die einzelnen LabelResultate der Kreuzvalididerung für diese Konfiguration.
	 * @param mar Die Macroaverage-Werte der Kreuzvalididerung für diese Konfiguration.
	 * @param mir Die Microaverage-Werte der Kreuzvalididerung für diese Konfiguration.
	 */
	private void printEvaluationResultsToFile(ClassificationConfig config, Integer[][] cm, List<LabelEvaluationResult> labelResults, MacroAverageResult mar, MicroAverageResult mir) {
		
		String tolerance = ""+config.getSequProbsTolerance();
		String bigrams = Boolean.toString(config.getUseBigramsInsteadOfTokens());
		String vectorType = config.getVectorType().name();
		String filePath = evalResultPath+"EvaluationForConfig_tolerance-"+tolerance+"_bigrams-"+bigrams+"_vectorType-"+vectorType+".txt";
		System.out.println("EVALUATION_APP: Generating evaluation result output and printing it to file '"+filePath+"'...");

		String matrixHeader = "   true\\pred   |";
		for(DiplomaticLabel label : labels){
			String name = " "+label.name();
			while(name.length() < 15){
				name += " ";
			}
			matrixHeader +=name + "|";
		}
		
		List<String> matrixLines = new ArrayList<>();
		for(int i = 0; i < labels.size(); i++){
			String line = labels.get(i).name();
			while(line.length() < 15){
				line += " ";
			}
			line += "|";
			for(int j = 0; j < labels.size(); j++){
				String value = " "+ cm[i][j];
				while(value.length() < 15){
					value += " ";
				}
				value += "|";
				line += value;
			}
			matrixLines.add(line);
		}
		
		List<DiplomaticLabel> unusedLabels = mar.getUnused();
		String unused = "";
		if(unusedLabels.size() == 0){ 
			unused = "No labels not included.";
		} else {
			for(DiplomaticLabel label : mar.getUnused()){
				unused += label.name()+"; ";
			}
		}
		
		DecimalFormat df = new DecimalFormat("#.####");
		
		List<String> lines = new ArrayList<>();
		lines.add("***EVALUATION RESULTS FOR CONFIG:***");
		lines.add("Tolerance for position based label probability: "+tolerance);
		lines.add("Use bigrams instead of tokens: "+bigrams);
		lines.add("Vectorizing method: "+vectorType);
		lines.add("Test basis: "+testData.size()+" diplomas with "+sentenceCount+ " sentences");
		lines.add(" ");
		lines.add(" ");

		lines.add("**CONFUSION MATRIX**");
		lines.add(matrixHeader);
		lines.addAll(matrixLines);
		lines.add(" ");
		lines.add(" ");

		lines.add("The following labels are not included in the macro- and microaverages because they don't appear in the training data:");
		lines.add(unused);
		lines.add(" ");
		
		lines.add("**MACROAVERAGES**");
		lines.add("Prec: "+df.format(mar.getPrecision()));
		lines.add("Rec: "+df.format(mar.getRecall()));
		lines.add("Acc: "+df.format(mar.getAccuracy()));
		lines.add("F1: "+df.format(mar.getF1Value()));
		lines.add(" ");
		lines.add(" ");
		
		lines.add("**MICROAVERAGES**");
		lines.add("Prec: "+df.format(mir.getPrecision()));
		lines.add("Rec: "+df.format(mir.getRecall()));
		lines.add("Acc: "+df.format(mir.getAccuracy()));
		lines.add("F1: "+df.format(mir.getF1Value()));
		lines.add(" ");
		lines.add(" ");
		
		lines.add("**DETAILED LABEL RESULTS**");
		for(LabelEvaluationResult result : labelResults){
			lines.add(result.getLabel().name()+":");
			lines.add("\tPrec:"+df.format(result.getPrecision()));
			lines.add("\tRec:"+df.format(result.getRecall()));
			lines.add("\tAcc:"+df.format(result.getAccuracy()));
			lines.add("\tF1:"+df.format(result.getF1Value()));
		}
		
		ReaderWriter.writeLineByLineToFile(lines, filePath);
		
	}
	
	private void printAverageEvaluationResultsToFile() {
		
		DecimalFormat df = new DecimalFormat("0000");
		String filePath = evalResultPath+"AllAverageEvaluationResults.txt";
		System.out.println("EVALUATION_APP: Generating total evaluation result output and printing it to file '"+filePath+"'...");

		String header1 = "_________Config__________"+"|"+"___________Macro___________"+"|"+"________Micro_________"+"|";
		String header2 = "_Vec,_Tolerance,_Bigrams_"+"|"+"_Prec_|_Rec__|_Acc__|__F1__"+"|"+"_Prec,_Rec,_F1_"+"|"+"_Acc__"+"|";
		
		List<String> lines = new ArrayList<>();
		lines.add("***AVERAGE EVALUATION RESULTS FROM"+(new Date())+":***");
		lines.add("'Vec' = Vectorizing method");
		lines.add("'Tolerance' = Tolerance for position based label probability");
		lines.add("'Bigrams' = have bigrams been used instead of tokens?");
		lines.add("Test basis: "+testData.size()+" diplomas with "+sentenceCount+ " sentences");
		lines.add(" ");
		lines.add(" ");


		lines.add("**MACRO- AND MICROAVERAGES FOR ALL TESTED CONFIGURATIONS**");
		lines.add("(Multiplied by 10.000)");
		lines.add(" ");
		lines.add(header1);
		lines.add(header2);
		
		for(Entry<ClassificationConfig, List<Double>> e : averageResults.entrySet()){
			
			ClassificationConfig config = e.getKey();
			List<Double> values = e.getValue();

			String tolerance = ""+config.getSequProbsTolerance();
			String bigrams = Boolean.toString(config.getUseBigramsInsteadOfTokens());
			String vectorType = config.getVectorType().name();

			String line = " "+vectorType+", "+tolerance+", "+bigrams;
			while(line.length() < 25) {
				line+=" ";
			}
			
			line+="|";
			
			for(int i = 0; i < values.size(); i++) {
				
				Double value = values.get(i);
				
				if(i == 4){
					line+="     "+df.format(value*10000)+"      |";
				} else{
					line+=" "+df.format(value*10000)+" |";
				}
			}

			lines.add(line);
		}
		
		ReaderWriter.writeLineByLineToFile(lines, filePath);
		
	}
}
