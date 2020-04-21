package classification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dataClasses.diploma.TrainingDiploma;
import dataClasses.label.DiplomaticLabel;
import dataClasses.sentence.AbstractSentence;
import dataClasses.sentence.TrainingSentence;

import preprocessing.Preprocessor;
import preprocessing.VectorType;

import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

/**
 * Der ProbabilisticClassifier verwaltet die Trainingsdaten des Programms. Mit diesen wird der Classifier trainiert.
 * Die Klasse bietet als einzige öffentliche Methode die classify()-Methode an.
 * Der eigentliche Classifier wird realisiert mithilfe der Java-Library des <b>Weka-Project der Waikato Universität Neuseeland</b>. Link zur
 * Projekt-Homepage: https://www.cs.waikato.ac.nz/ml/index.html (zuletzt aufgerufen 20.08.2019)
 * @author Alina Ostrowski
 *
 */
public class ProbabilisticClassifier {

	private List<TrainingDiploma> trainingDiplomas;
	private List<TrainingSentence> trainingSentences;
	
	/**
	 * Der Classifier stammt aus der Weka-Library. Es handelt sich um einen multinomialen NaiveBayes-Classifier, d.h. er
	 * ist dafür optimiert, Multilabel-Classification zu unterstützen.
	 */
	private NaiveBayesMultinomial nbm;
	private Instances trainingInstances;
	
	private Preprocessor pp;
	private VectorType vectorType;
	private List<String> typeVector;
	private boolean useBigramsInsteadOfTokens;
	private Map<String, Integer> totalTypeFrequencies;
	
	private List<String> labelList;

	public ProbabilisticClassifier(Preprocessor pp, List<TrainingDiploma> trainingData, boolean useBigramsInsteadOfTokens, VectorType vectorType){
		this.pp = pp;
		nbm = new NaiveBayesMultinomial();
		this.useBigramsInsteadOfTokens = useBigramsInsteadOfTokens;
		this.vectorType = vectorType;

		labelList = new ArrayList<>();
		DiplomaticLabel[] dl = DiplomaticLabel.values();
		for(DiplomaticLabel label : dl){
			labelList.add(label.name());
		}
		
		trainingDiplomas = trainingData;
		trainingSentences = new ArrayList<>();
		
		for(TrainingDiploma dipl : trainingDiplomas){
			trainingSentences.addAll(dipl.getSentences());
		}
		
		// Vorbereitung der Trainingsdaten und Trainierung des NaiveBayes-Classifiers
		System.out.println("PROBABILISTIC_CLASSIFIER: Preparing training sentences for classifier training (lemmatizing, vectorizing)...");
		List<AbstractSentence> abstractList = new ArrayList<AbstractSentence>(trainingSentences);
		pp.lemmaTokenizeSentences(abstractList);
		if(useBigramsInsteadOfTokens){
			pp.lemmatizedTokensToBigrams(abstractList);
		}
		
		totalTypeFrequencies = pp.createTypeFrequenciesMap(trainingSentences, useBigramsInsteadOfTokens);
		typeVector = new ArrayList<>(totalTypeFrequencies.keySet());
		
		pp.vectorize(new ArrayList<AbstractSentence>(trainingSentences), useBigramsInsteadOfTokens, vectorType, totalTypeFrequencies, trainingSentences.size(), typeVector);

		System.out.println("PROBABILISTIC_CLASSIFIER: Training classifier...");
		trainClassifier();
		
	}

	/**
	 * Bereitet alle Trainingsdaten des ProbabilisticClassifiers für die Übergabe an den Weka-NaiveBayes-Classifier vor
	 * und und ruft NaiveBayesMultinomial.buildClassifier() zum Trainieren des Classifiers auf.
	 */
	private void trainClassifier() {
		
		Attribute labelAtt = new Attribute("Label", labelList);		
		
		ArrayList<Attribute> structureVector = new ArrayList<Attribute>();
		
		for (String f : typeVector) {
			structureVector.add(new Attribute(f));
		}
		structureVector.add(labelAtt);

		
		trainingInstances = new Instances("TrainingInstances", structureVector, typeVector.size() + 1);
		trainingInstances.setClassIndex(structureVector.size()-1);

		// für jeden Satz ein Weka-Attribute erzeugen und das Label setzen
		for (TrainingSentence sent : trainingSentences) {
			String label = sent.getTruePartLabel().name();
			double[] vector = sent.getVector();
			
			Instance i = new SparseInstance(1.0, vector);
			i.setDataset(trainingInstances);
			i.setClassValue(label);
		
			// das Weka-Attribut den Instances zum Trainieren übergeben
			trainingInstances.add(i);
		}

		try {
			// Classifier trainieren
			nbm.buildClassifier(trainingInstances);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
	/**
	 * Bereitet die übergebenen Sätze für die Klassifizierung durch den Weka-NBM-Classifier vor und lässt für jeden Satz die
	 * Labelwahrscheinlichkeiten berechnen. Die so erhaltenen Labelwahrscheinlichkeiten werden mit den bereits bestehenden
	 * Labelwahrscheinlichkeiten der Sätze verrechnet.
	 * @param sentences Die zu klassifizierenden Sätze.
	 */
	public void classify(List<AbstractSentence> sentences) {

		System.out.println("PROBABILISTIC_CLASSIFIER: Preparing sentences for classification (lemmatizing, vectorizing)...");
		pp.lemmaTokenizeSentences(sentences);
		if(useBigramsInsteadOfTokens){
			pp.lemmatizedTokensToBigrams(sentences);
		}
		pp.vectorize(sentences, useBigramsInsteadOfTokens, vectorType, totalTypeFrequencies, trainingSentences.size(), typeVector);
		
		for(AbstractSentence sent : sentences){
			sent.updateLabelProbability(classifyByNaiveBayes(sent));
		}
		
	}

	private double[] classifyByNaiveBayes(AbstractSentence sent) {
		
		double[] vector = sent.getVector();
		
		Instance i = new SparseInstance(1.0, vector);
		i.setDataset(trainingInstances);
		i.setClassMissing();
		double[] labelProbs;
		try {
			labelProbs = nbm.distributionForInstance(i);
		} catch (Exception e) {
			double[] standardProbs = new double[DiplomaticLabel.values().length];
			for(int j = 0; j < standardProbs.length; j++){
				standardProbs[j] = 1;
			}
			e.printStackTrace();
			return standardProbs;
		}
		
		return labelProbs;		
	}
	
}
