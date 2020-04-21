package config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dataClasses.diploma.TrainingDiploma;
import dataClasses.label.DiplomaticLabel;
import dataClasses.label.DiplomaticParagraphLabel;
import dataClasses.sentence.TrainingSentence;

/**
 * Berechnet und enthält durchschnittliche Werte für die Länge einzelner Urkundenabschnitte.
 * @author Alina Ostrowski
 *
 */
public class Milestones {

	private List<TrainingDiploma> diplomas;
	private int tolerance;
	private List<DiplomaticLabel> labels = Arrays.asList(DiplomaticLabel.values());
	
	private double averageProtocolEnd;
	private double inversedAverageEschatocolStart;
	private double[] diplomaPartProbabilities;
	private double[] protocolDiplomaPartProbabilities;
	private double[] contextDiplomaPartProbabilities;
	private double[] eschatocolDiplomaPartProbabilities;
	
	/**
	 * Initialisiert Default-Werte.<br>
	 * <b>Achtung:</b> Dieser Konstruktor sollte nur aufgerufen werden, wenn die dadurch erzeugte Instanz vor dem tatsächlichen Zugriff
	 * auf ihre Methoden durch eine andere, mit einem anderen Konstruktor erstellte Instanz ersetzt wird. Die hier erzeugten Default-Werte
	 * sind unveränderlich und werden bei einer Verwendung im besten Falle keinerlei Einfluss haben, im schlechtesten Falle zu falschen Ergebnissen
	 * führen.
	 */
	public Milestones(){
		this.averageProtocolEnd = Integer.MAX_VALUE;
		this.inversedAverageEschatocolStart = Integer.MAX_VALUE;
		this.diplomaPartProbabilities = newProbsArray(1.0);
		this.protocolDiplomaPartProbabilities = newProbsArray(1.0);
		this.contextDiplomaPartProbabilities = newProbsArray(1.0);
		this.eschatocolDiplomaPartProbabilities = newProbsArray(1.0);
	}
	
	/**
	 * Initialisiert Werte, die anhand der übergebenen Trainingsurkunden berechnet werden.
	 * @param diplomas Die TrainingDiploma-Objekte, auf deren Basis die Member-Werte berechnet werden sollen.
	 * @param tolerance Der Toleranzwert für die Berechnung der abschnittsabhängigen diplomaPartProbabilities.
	 */
	public Milestones(List<TrainingDiploma> diplomas, int tolerance){
		this.diplomas = diplomas;
		this.tolerance = tolerance;
		this.calculateAverageParagraphLengths();
		this.diplomaPartProbabilities = this.calculateDiplomaPartProbabilities();
		this.protocolDiplomaPartProbabilities = this.calculateDiplomaPartProbabilitiesByParagraph(DiplomaticParagraphLabel.protocol, diplomaPartProbabilities);
		this.contextDiplomaPartProbabilities = this.calculateDiplomaPartProbabilitiesByParagraph(DiplomaticParagraphLabel.context, diplomaPartProbabilities);
		this.eschatocolDiplomaPartProbabilities = this.calculateDiplomaPartProbabilitiesByParagraph(DiplomaticParagraphLabel.eschatocol, diplomaPartProbabilities);
	}

	/**
	 * Berechnet die durchschnittliche absolute Wortlänge der 3 Urkundenabschnitte Protokoll, Kontext und Eschatokoll anhand der Grenzen zwischen den Abschnitten basierend auf dem
	 * wordIndex der sie begrenzenden Sätze. Zur Berechnung werden die Urkunden des Milestones-Objektes genutzt.
	 */
	private void calculateAverageParagraphLengths() {
		double protocolEndSum = 0;
		double eschatocolStartSum = 0;
		
		for(TrainingDiploma dipl : diplomas){
			int protocolEnd = 0;
			int inversedEschatocolStart = 0;
			List<TrainingSentence> diplSents = dipl.getSentences();
			
			//LastWordIndex des letzten Satzes des Protokolls finden
			for(int i = 0; i < diplSents.size(); i++){
				TrainingSentence sent = diplSents.get(i);
				if(sent.getTrueParagraphLabel() == DiplomaticParagraphLabel.values()[0]){
					protocolEnd = sent.getIndexOfLastWord();
				} else {
					break;
				}
			}
			
			//Invertierten FirstWortIndex des ersten Satzes des Eschatokolls finden
			for(int i = diplSents.size()-1; i > 0; i--){
				TrainingSentence sent = diplSents.get(i);
				if(sent.getTrueParagraphLabel() == DiplomaticParagraphLabel.values()[2]){
					inversedEschatocolStart = sent.getInversedIndexOfFirstWord();
				} else {
					break;
				}
			}
			
			protocolEndSum+=protocolEnd;
			eschatocolStartSum+=inversedEschatocolStart;
		}
		
		// Relative Protokollenden und Eschatokollstarts aller Urkunden mitteln
		int total = diplomas.size();
		averageProtocolEnd = (double) protocolEndSum / total;
		inversedAverageEschatocolStart = (double) eschatocolStartSum / total;
	}
	
	/**
	 * Berechnet die Wahrscheinlichkeit, mit der ein zufällig gewählter Satz einer Urkunde zu einem bestimmten Urkundenelement (Label) gehört.
	 * Dafür wird die durchschnittliche Länge (basierend auf dem relativen WortIndex der Sätze) der einzelnen Elemente in den Urkunden des Milestones-Objektes genutzt.
	 * @return Ein double-Array mit den Wahrscheinlichkeitswerten der einzelnen Urkundenelemente.
	 */
	private double[] calculateDiplomaPartProbabilities() {
		
		// Array mit Wahrscheinlichkeitssummen initialisieren
		double[] partProbSums = newProbsArray(0.0);
		
		int total = diplomas.size();
		// Berechnung der relativen Wahrscheinlichkeiten aller Sätze pro Urkunde
		for(TrainingDiploma dipl : diplomas){
			List<TrainingSentence> diplSents = dipl.getSentences();
			updatePartProbSums(partProbSums, diplSents, 0, 0, dipl.getTotalWordCount());
		}

		// Relative Label-Wahrscheinlichkeiten mitteln
		double[] partProbabilities = new double[labels.size()];
		for(int i = 0; i < partProbSums.length; i++){
			partProbabilities[i] = partProbSums[i] / total;
		}
		
		return partProbabilities;
	}
	
	/**
	 * Berechnet die Wahrscheinlichkeit, mit der ein zufällig gewählter zu einem bestimmten Abschnitt gehörender Satz einer Urkunde zu einem bestimmten Urkundenelement (Label) gehört.
	 * Dafür wird die durchschnittliche Länge (basierend auf dem relativen WortIndex der Sätze) der einzelnen Elemente in den Urkunden des Milestones-Objektes genutzt.
	 * @param paragraph Das Label des Abschnitts, zu dem der Satz gehört.
	 * @param diplomaPartProbabilities Die Wahrscheinlichkeiten aller Label unabhängig von ihrer Zugehörigkeit zu einem bestimmten Abschnitt.
	 * @return Ein double-Array mit den Wahrscheinlichkeitswerten der einzelnen Urkundenelemente.
	 */
	private double[] calculateDiplomaPartProbabilitiesByParagraph(DiplomaticParagraphLabel paragraph, double[] diplomaPartProbabilities){
		
		// festlegen, welche Label zum betrachteten Paragraphen gehören
		int firstLabel = 0;
		int lastLabel = 11;
		
		switch(paragraph){
			case protocol: firstLabel = 0;
				lastLabel= 2;
				break;
			case context: firstLabel = 3;
				lastLabel= 8;
				break;
			case eschatocol: firstLabel = 9;
				lastLabel= 11;
				break;
			default: break;
		}
		
		// Index des ersten und letzten Toleranzlabels berechnen
		int toleranceMin = firstLabel - tolerance;
			if(toleranceMin < 0) toleranceMin = 0;
		int toleranceMax = lastLabel + tolerance;
			if(toleranceMax > 11) toleranceMax = 11;
		

		// Array mit Wahrscheinlichkeitssummen initialisieren
		double[] partProbSums = newProbsArray(0.0);
		
		for(TrainingDiploma dipl : diplomas){

			// Für jede Urkunde diejenigen Sätze heraussuchen, die zum aktuell betrachteten Paragraphen gehören
			// sowie als Toleranzsätze zusätzlich noch diejenigen, die zu den Toleranzlabeln gehören
			List<TrainingSentence> diplSents = dipl.getSentences();
			List<TrainingSentence> paragraphSentences = new ArrayList<>();
			int wordCount = 0;
			List<TrainingSentence> preToleranceSentences = new ArrayList<>();
			int preToleranceWordCount = 0;
			List<TrainingSentence> afterToleranceSentences = new ArrayList<>();
			int afterToleranceWordCount = 0;
			
			for(int i = 0; i < diplSents.size(); i++){

				TrainingSentence sent = diplSents.get(i);
				int sentLabelIndex = labels.indexOf(sent.getTruePartLabel());
				DiplomaticParagraphLabel paragraphLabel = sent.getTrueParagraphLabel();

				if(paragraphLabel == paragraph){
					paragraphSentences.add(sent);
					wordCount += sent.getLemmatizedTokens().size();
				} else if(sentLabelIndex >= toleranceMin && sentLabelIndex < firstLabel){
					preToleranceSentences.add(sent);
					preToleranceWordCount += sent.getLemmatizedTokens().size();
				} else if(sentLabelIndex <= toleranceMax && sentLabelIndex > lastLabel){
					afterToleranceSentences.add(sent);
					afterToleranceWordCount += sent.getLemmatizedTokens().size();
				}
			}
			
			int toleranceCount = preToleranceWordCount + afterToleranceWordCount+ wordCount;
			

			// Für alle Paragraph-Sätze sowie alle Toleranz-Sätze (wenn es sie gibt) neue, erhöhte Label-Wahrscheinlichkeiten berechnen,
			// dabei als relativen Index den Index des Satzes innerhalb des Paragraphen (bzw. innerhalb des Paragraphen + Toleranzsätze für die Toleranzlabel)
			// dividiert durch die Paragraphenlänge (bzw. Paragraphenlänge + Toleranzläbellänge) nutzen.
			
			updatePartProbSums(partProbSums, paragraphSentences, firstLabel, 0, wordCount);
			
			if(!preToleranceSentences.isEmpty()){
				updatePartProbSums(partProbSums, preToleranceSentences, toleranceMin, 0, toleranceCount);
			}
			
			if(!afterToleranceSentences.isEmpty()){
				updatePartProbSums(partProbSums, afterToleranceSentences, lastLabel+1, (preToleranceWordCount+wordCount), toleranceCount);
			}
			
		}

		// Die bereits berechneten allgemeinen Label-Wahrscheinlichkeiten aktualisieren, indem die Wahrscheinlichkeiten
		// der betrachteten Paragraphen-Label sowie der Toleranzlabel durch die neu berechneten Wahrscheinlichkeiten ersetzt werden
		int total = diplomas.size();
		double[] paragraphRelativeProbabilities = new double[labels.size()];
		for(int i = 0; i < labels.size(); i++){
			if(i >= toleranceMin && i <= toleranceMax){
				paragraphRelativeProbabilities[i] = partProbSums[i] / total;
			} else {
				paragraphRelativeProbabilities[i] = diplomaPartProbabilities[i];
			}
		}
		
		return paragraphRelativeProbabilities;
	}
	
	/**
	 * Berechnet für alle übergebenen Sätze ihre Länge relativ zur Gesamtlänge (totalWordCount) ihres Abschnitts und berechnet auf Basis
	 * dessen die Gesamtwahrscheinlichkeit für das Auftreten bestimmter Label.
	 * @param partProbSums Wahrscheinlichkeitsarray, dessen Werte mit den neu berechneten Wahrscheinlichkeiten aktualisiert werden sollen.
	 * @param sentences Die Sätze, anhand derer die Labelwahrscheinlichkeiten berechnet werden sollen.
	 * @param indexOfStartLabel Das erste Label, für das eine neue Wahrscheinlichkeit berechnet werden soll. Wahrscheinlichkeien werden für alle Label berechnet,
	 * deren Index höher als der des indexOfStartLabel ist und die in den sentences auftauchen.
	 * @param wordIndexModifier Wie soll der eigentliche Index der Sätze modifiziert werden, damit er in der gewünschten Relation zur Gesamtlänge steht?
	 * @param totalWordCount Die Gesamtlänge des Abschnitts, in Relation dessen die Wahrscheinlichkeiten der Label berechnet werden sollen.
	 */
	private void updatePartProbSums(double[] partProbSums, List<TrainingSentence> sentences, int indexOfStartLabel, int wordIndexModifier, int totalWordCount){
		int currentPartIndex = indexOfStartLabel;
		double partStart =  (double) wordIndexModifier / (double) totalWordCount;
		double partEnd = (double) wordIndexModifier / (double) totalWordCount;
		int wordIndex = wordIndexModifier;
		for(int i = 0; i < sentences.size(); i++){
			TrainingSentence sent = sentences.get(i);
			
			wordIndex += sent.getLemmatizedTokens().size();
			double wordRelativeIndex = (double) (wordIndex) / totalWordCount;
			DiplomaticLabel sentLabel = sent.getTruePartLabel();
			if(sentLabel == labels.get(currentPartIndex)){
				partEnd = wordRelativeIndex;
			} else {
				double partProb = partEnd - partStart;
				partProbSums[currentPartIndex]+=partProb;
				partStart = partEnd;
				partEnd = wordRelativeIndex;
				currentPartIndex = labels.indexOf(sentLabel);
			}
		}
		double partProb = partEnd - partStart;
		partProbSums[currentPartIndex]+=partProb;	
	}
	
	/**
	 * @param initialValue Der Wert, den alle Stellen des Arrays erhalten sollen.
	 * @return Ein double-Array derselben Länge wie {@link #labels}, in dem alle Werte gleich dem initialValue sind.  
	 */
	private double[] newProbsArray(double initialValue){
		double[] partProbs = new double[labels.size()];
		for(int i = 0; i < partProbs.length; i++){
			partProbs[i] = initialValue;
		}
		return partProbs;
	}
	
	/**
	 * 
	 * @return Der durchschnittliche lastWordIndex des Protokolls.
	 */
	public double getAverageProtocolEnd() {
		return averageProtocolEnd;
	}

	/**
	 * @return Der durchschnittliche inversedFirstWordIndex des Eschatokolls.
	 */
	public double getInversedAverageEschatocolStart() {
		return inversedAverageEschatocolStart;
	}

	/**
	 * @return Die durchschnittliche relative Länge der einzelnen Label-Abschnitte. Die Länge und Ordnung des double-Arrays entspricht denen des
	 * Arrays, das durch DiplomaticLabel.values() erzeugt wird.
	 */
	public double[] getDiplomaPartProbabilities() {
		return diplomaPartProbabilities;
	}

	/**
	 * @return Die durchschnittliche relative Länge der einzelnen Label-Abschnitte, wobei jedoch die durchschnittliche
	 * Länge der Label-Abschnitte des Protokolls höher gewichtet werden. Die Länge und Ordnung des double-Arrays entspricht denen des
	 * Arrays, das durch DiplomaticLabel.values() erzeugt wird.
	 */
	public double[] getProtocolDiplomaPartProbabilities() {
		return protocolDiplomaPartProbabilities;
	}

	/**
	 * @return Die durchschnittliche relative Länge der einzelnen Label-Abschnitte, wobei jedoch die durchschnittliche
	 * Länge der Label-Abschnitte des Kontextes höher gewichtet werden. Die Länge und Ordnung des double-Arrays entspricht denen des
	 * Arrays, das durch DiplomaticLabel.values() erzeugt wird.
	 */
	public double[] getContextDiplomaPartProbabilities() {
		return contextDiplomaPartProbabilities;
	}

	/**
	 * @return Die durchschnittliche relative Länge der einzelnen Label-Abschnitte, wobei jedoch die durchschnittliche
	 * Länge der Label-Abschnitte des Eschatokolls höher gewichtet werden. Die Länge und Ordnung des double-Arrays entspricht denen des
	 * Arrays, das durch DiplomaticLabel.values() erzeugt wird.
	 */
	public double[] getEschatocolDiplomaPartProbabilities() {
		return eschatocolDiplomaPartProbabilities;
	}
	
	
}
