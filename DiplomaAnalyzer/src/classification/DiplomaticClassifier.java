package classification;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import config.Milestones;
import dataClasses.diploma.AbstractDiploma;
import dataClasses.label.DiplomaticLabel;
import dataClasses.label.DiplomaticParagraphLabel;
import dataClasses.sentence.AbstractSentence;
import helpers.ReaderWriter;
import preprocessing.Preprocessor;

/**
 * Die Klasse bündelt alle Methoden zur Klassifikation einzelner Textabschnitte
 * mithilfe von festen Regeln der Diplomatik (d.h. nicht-probabilistische Methoden).
 * @author Alina Ostrowski
 *
 */
public class DiplomaticClassifier {
	
	/**
	 * Enthält Arrays mit Indikatoren (Merkmalen), die auf ein bestimmtes Label hinweisen. Damit die Indikatoren nutzbar sind, darf pro
	 * Array nur ein Indikator übergeben werden und die einzelnen Werte des Arrays müssen folgendermaßen aufgebaut sein: <br>
	 * <ol>
	 * <li>Indikator: String, den ein Satz enthalten muss, um dem Label zugeordnet zu werden.</li>
	 * <li>Label: String mit dem Namen desjenigen Labels, dem Sätze, die den Indikator enthalten, erhalten sollen.</li>
	 * <li>Ähnlichkeitsgrad (Ziffer): Wie hoch muss die Ähnlichkeit zwischen dem Indikator und dem Satz sein, damit die Regel greift?
	 * 		<ul><li> 1 = Substring: Der Indikatorsatz ist ein Substring des Originalsatzes, d.h. der Originalsatz enthält alle Wörter des Indikator-Strings in der richtigen Reihenfolge und ohne Unterbrechungen.
	 *			 		<br>Bsp.: "Ich esse heute" = "Ich esse heute Fisch" aber != "Fisch esse ich heute" und != "Ich esse" </li>
	 *			<li> 2 = Subset: Der Indikatorsatz ist ein Subset des Originalsatzes, d.h. der Originalsatz enthält alle Wörter des Indikator-Strings.
	 *					<br>Bsp.: "Ich gehe zum Fisch Essen." = "Mit meiner Freundin gehe ich zum Fisch Essen auf Norderney", aber != "Ich gehe zum Essen"</li>
	 *			<li> 3 = Similar: Der Indikatorsatz ähnelt einem Substring des Originalsatzes (d.h. Jaccard-Koeffizient >= 0.8).
	 *					<br>Bsp.: "Ich gehe zum Fisch Essen." = "Eine beliebig lange Wortfolge vor dem eigentlichen Substring; Ich gehe mittags Fische Essen, dann eine beliebig lange Wortfolge nach dem eigentlichen Substring."</li>
	 *			<li> default = Similar</li></ul></li>
	 * <li>Positionsbedingung (Ziffer): In Verbindung mit welcher Position des Satzes innerhalb der Urkunde gilt diese Indikatorregel?
	 * 		<ul><li> 1 = Satz muss innerhalb des Protokolls stehen </li>
				<li> 2 = Satz muss innerhalb des Kontextes stehen </li>
				<li> 3 = Satz muss innerhalb des Eschatokolls stehen </li>
				<li> 0 = der Satz erhält keine bestimmte Positionsbedingung </li></ul></li>
	 * </ol> 
	 */
	private List<String[]> indicators;
	
	private Milestones milestones;
	private double averageProtocolEnd;
	private double inversedAverageEschatocolStart;
	
	private SimilarityCalculator sc;

	public DiplomaticClassifier(String indicatorPath, Milestones ms){
		initializeIndicators(indicatorPath);
		sc = new SimilarityCalculator();
		this.setMilestones(ms);
	}

	/**
	 * Liest die diplomatischen Indikatoren in der Datei mit dem übergebenen Pfad ein und speichert sie
	 * als Feldvariable indicators. Der Vergleichsstring eines jeden Indikatorarrays wird dabei normalisiert.
	 * @param indicatorPath Der Pfad, unter dem sich die Datei mit den Indikatoren befindet.
	 */
	private void initializeIndicators(String indicatorPath) {
		indicators = ReaderWriter.readCSV(new File(indicatorPath), true, ",", 4);
		for(String[] indicatorArray : indicators){
			indicatorArray[0] = Preprocessor.normalizeLatinText(indicatorArray[0]);
			for(int i = 1; i <indicatorArray.length; i++){
				indicatorArray[i] = indicatorArray[i].trim();
			}
		}
	}
	
	/**
	 * Initialisiert die Milestones dieses Objekts.
	 * @param ms Das Milestones-Objekt, das für die Initialisierung genutzt werden soll.
	 */
	public void setMilestones(Milestones ms){
		this.milestones = ms;
		this.averageProtocolEnd = ms.getAverageProtocolEnd();
		this.inversedAverageEschatocolStart = ms.getInversedAverageEschatocolStart();
	}
	

	/**
	 * Aktualisiert die Label-Wahrscheinlichkeiten der übergebenen Satz-Objekte abhängig von ihrer Position innerhalb der Urkunde.
	 * Dafür werden die Paragraphen-abhängigen Label-Wahrscheinlichkeiten der probs-Map des Diplomatic-Classifier-Objekts genutzt.
	 * @param allSentences Die Sätze, deren Label-Wahrscheinlichkeiten aktualisiert werden sollen.
	 */
	public void assignSequenceBasedPropabilities(List<AbstractSentence> allSentences){
		double[] protocolProbs = milestones.getProtocolDiplomaPartProbabilities();
		double[] contextProbs = milestones.getContextDiplomaPartProbabilities();
		double[] eschatocolProbs = milestones.getEschatocolDiplomaPartProbabilities();
		
		for(AbstractSentence sent : allSentences){
			
			int inversedLastWordIndex = sent.getInversedIndexOfLastWord();
			int firstWordIndex = sent.getIndexOfFirstWord();
			if(firstWordIndex < averageProtocolEnd){ // gehört der Satz vermutlich zum Protokoll?
				sent.updateLabelProbability(protocolProbs);
			} else if(inversedLastWordIndex < inversedAverageEschatocolStart){ // gehört der Satz vermutlich zum Eschatokoll?
				sent.updateLabelProbability(eschatocolProbs);
			} else { // gehört der Satz vermutlich zum Kontext?
				sent.updateLabelProbability(contextProbs);
			}
		}
	}	

	/**
	 * Weist denjenigen ungelabelten Sätzen einer Urkunde, die zwischen zwei Sätzen, die
	 * dasselbe Label haben, stehen, das Label der umrahmenden Sätze zu. <br>
	 * <b> Bsp: </b> dispositio, null, null, dispositio -> dispositio, dispositio, dispositio, dispositio <br>
	 * <b> Aber: </b> dispositio, null, null, corroboratio -> dispositio, null, null, corroboratio
	 * @param diplomas Die Urkunden, deren Sätze untersucht werden sollen.
	 */
	public void fillSequence(List<AbstractDiploma> diplomas) {

		for(AbstractDiploma<AbstractSentence> dipl : diplomas){
			
			List<AbstractSentence> sentences = dipl.getSentences();
			DiplomaticLabel lastLabel = DiplomaticLabel.invocatio;
			List<AbstractSentence> currentSentences = new ArrayList<>();
			
			for(AbstractSentence sentence : sentences){
				DiplomaticLabel sentLabel = sentence.getLabel();
				if(sentLabel == null){
					currentSentences.add(sentence);
				} else if(sentLabel == lastLabel){
					for(AbstractSentence currentSent : currentSentences){
						currentSent.setPartLabel(lastLabel);
					}
					currentSentences.clear();
				} else {
					lastLabel = sentLabel;
					currentSentences.clear();
				}
			}
		}
	}
	
	/**
	 * Weist allen bisher ungelabelten Sätzen einer Urkunde ein Label zu basierend auf den Labeln ihrer bereits klassifizierten Nachbarsätze sowie
	 * der satzeigenen Label-Wahrscheinlichkeit. Dabei ist sichergestellt, dass die Reihenfolge aller möglichen Label beibehalten wird, auch,
	 * wenn dadurch ein Satz ein Label mit einer bedeutend geringen Wahrscheinlichkeiten erhält. 
	 * @param diplomas Die Urkunden, deren Sätze gelabelt werden sollen.
	 */
	public void assignByDefault(List<AbstractDiploma> diplomas) {
		
		for(AbstractDiploma<AbstractSentence> dipl : diplomas){
			if(dipl.getFileName().equals("1189_V_18.cei.xml")) {
				System.out.println("1189");
			}
			DiplomaticLabel lastLabel = DiplomaticLabel.invocatio;
			DiplomaticLabel nextLabel = DiplomaticLabel.apprecatio;
			
			// in currentNonLabeledSentences werden alle Sätze gespeichert, die zwischen dem letzten eingelesenen Label
			// und dem nächsten eingelesenen Label stehen
			List<AbstractSentence> currentNonLabeledSentences = new ArrayList<>();
			for(AbstractSentence sent : dipl.getSentences()){
				if(sent.hasLabel()){
					DiplomaticLabel currentLabel = sent.getLabel();
					if(currentNonLabeledSentences.size() == 0){
						lastLabel = currentLabel;
					} else{
						nextLabel = currentLabel;
						// alle bisher ungelabelten Sätze labeln und die nun gelabelten Sätze aus der Liste entfernen
						labelSentencesByLabelProb(currentNonLabeledSentences, lastLabel, nextLabel);
						currentNonLabeledSentences.clear();
						lastLabel = currentLabel;
					}
				} else {
					currentNonLabeledSentences.add(sent);
				}
			}
			labelSentencesByLabelProb(currentNonLabeledSentences, lastLabel, DiplomaticLabel.apprecatio);
		}
	}

	/**
	 * Weist den übergebenen Sätzen dasjenige Label zu, welches die höchste Wahrscheinlichkeit für diesen Satz aufweist und dabei
	 * gleichzeitig weder vor dem übergebenen firstLabel noch nach dem übergebenen lastLabel steht.  
	 * @param currentNonLabeledSentences Diejenigen Sätze, die ein Label erhalten sollen.
	 * @param lastLabel Das erste mögliche Label.
	 * @param nextLabel Das letzte mögliche Label.
	 */
	private void labelSentencesByLabelProb(List<AbstractSentence> currentNonLabeledSentences, DiplomaticLabel lastLabel, DiplomaticLabel nextLabel) {

		List<DiplomaticLabel> labelList = Arrays.asList(DiplomaticLabel.values());
		
		for(AbstractSentence sent : currentNonLabeledSentences){
			double[] labelProbs = sent.getLabelProbabilities();

			double highestProb = 0;
			
			DiplomaticLabel mostProbableLabel = lastLabel;
			
			for(int i = labelList.indexOf(lastLabel); i <= labelList.indexOf(nextLabel); i++){
				double prob = labelProbs[i];
				if(prob > highestProb){
					highestProb = prob;
					mostProbableLabel = labelList.get(i);
				}
			}
			
			sent.setPartLabel(mostProbableLabel);
			
			// nachdem dem Satz ein Label zugewiesen wurde, wird dieses Label zum neuen nach unten hin begrenzenden Label
			lastLabel = mostProbableLabel;
		}
	}
	
	/**
	 * Wendet die zuvor für die Instanz dieser Klasse festgelegten Indikatoren auf die übergebenen Sätze an und weist ihnen ein entsprechendes Label zu,
	 * falls einer der Indikatoren zutrifft.
	 * @param sentences Die Sätze, für die ein Indikator-Labeling geprüft werden soll 
	 * @return Eine Liste mit allen Satzobjekten, denen im Zuge der Indikator-Überprüfung noch kein Label zugewiesen werden konnte.
	 */
	public List<AbstractSentence> assignByIndicators(List<AbstractSentence> sentences) {

		List<AbstractSentence> labeledSentences = new ArrayList<>();
		List<AbstractSentence> unlabeledSentences = sentences;
		
		for(AbstractSentence sentence : sentences){
			
			String text = sentence.getText();
			int inversedLastWordIndex = sentence.getInversedIndexOfLastWord();
			int inversedFirstWordIndex = sentence.getInversedIndexOfFirstWord();
			int lastWordIndex = sentence.getIndexOfLastWord();
			int firstWordIndex = sentence.getIndexOfFirstWord();
		
			indicatorLoop: for(String[] indicatorPair : indicators){
				
				// Überprüfen, ob die Indikatorregel nur für Sätze mit einer bestimmten Position innerhalb der Urkunde gilt:
				// 1 = Satz muss innerhalb des Protokolls stehen
				// 2 = Satz muss innerhalb des Kontextes stehen
				// 3 = Satz muss innerhalb des Eschatokolls stehen
				// 0 oder default = der Satz erhält keine bestimmte Positionsbedingung
				// Entspricht der aktuelle Satz der Bedingung nicht, so wird die aktuelle Iteration übersprungen
				// und der nächste Indikator überprüft.
				
				int position = Integer.valueOf(indicatorPair[3]);
				if(position != 0){
					switch (position){
					case 1: 
						if(firstWordIndex > averageProtocolEnd){
							continue indicatorLoop;
						}
						break;
					case 2:
						if(lastWordIndex < averageProtocolEnd || inversedFirstWordIndex < inversedAverageEschatocolStart){
							continue indicatorLoop;
						}
						break;
					case 3:
						if(inversedLastWordIndex > inversedAverageEschatocolStart){
							continue indicatorLoop;
						}
						break;
					default:
						break;
					}
				}
			
				// Überprüfen, welcher Grad an Ähnlichkeit vorliegen soll
				// 1 = Substring: Der Indikatorsatz ist ein Substring des Originalsatzes, d.h. der Originalsatz enthält alle Wörter des Indikator-Strings in der richtigen Reihenfolge und ohne Unterbrechungen.
				// 		Bsp.: "Ich esse heute" = "Ich esse heute Fisch" aber != "Fisch esse ich heute" und != "Ich esse" 
				// 2 = Subset: Der Indikatorsatz ist ein Subset des Originalsatzes, d.h. der Originalsatz enthält alle Wörter des Indikator-Strings.
				//		Bsp.: "Ich gehe zum Fisch Essen." = "Mit meiner Freundin gehe ich zum Fisch Essen auf Norderney", aber != "Ich gehe zum Essen"
				// 3 = Similar: Der Indikatorsatz ähnelt einem Substring des Originalsatzes (d.h. Jaccard-Koeffizient >= 0.75).
				//		Bsp.: "Ich gehe zum Fisch Essen." = "Eine beliebig lange Wortfolge vor dem eigentlichen Substring; Ich gehe mittags Fische Essen, dann eine beliebig lange Wortfolge nach dem eigentlichen Substring."
				// default = Similar
				// Trifft der Indikator zu und es wird ein Label gefunden, so sollen die nachfolgenden Indikatoren nicht mehr abgeprüft werden,
				// sondern die Indikator-Schleife wird abgebrochen und der nächste Satz untersucht.
				
				int similarity = Integer.valueOf(indicatorPair[2]);
				switch(similarity){
				case 1:
					if(sc.containsSimilarTokenSubstring(indicatorPair[0], text)){
						sentence.setPartLabel(DiplomaticLabel.valueOf(indicatorPair[1]));
						labeledSentences.add(sentence);
						break indicatorLoop;
					} else break;
				case 2:
					if(sc.containsAllTokens(indicatorPair[0], text)){
						sentence.setPartLabel(DiplomaticLabel.valueOf(indicatorPair[1]));
						labeledSentences.add(sentence);
						break indicatorLoop;
					} else break;
				case 3:
					if(sc.similarityOfSubstring(indicatorPair[0], text)>=0.75){
						sentence.setPartLabel(DiplomaticLabel.valueOf(indicatorPair[1]));
						labeledSentences.add(sentence);
						break indicatorLoop;
					} else break;
				default: break;
				}
			}
		}
		
		unlabeledSentences.removeAll(labeledSentences);
		return unlabeledSentences;
	}

	/**
	 * Weist den übergebenen Sätzen das zu ihrem Part-Label passende Paragraphen-Label zu.
	 * @param sentences Die Sätze, denen ein Paragraphen-Label hinzugefügt werden soll.
	 */
	public void assignParagraphsByPartLabels(List<AbstractSentence> sentences) {

		List<DiplomaticLabel> labels = Arrays.asList(DiplomaticLabel.values());
		DiplomaticParagraphLabel[] paragraphLabels = DiplomaticParagraphLabel.values();
		
		for(AbstractSentence sent : sentences){
			DiplomaticLabel label = sent.getLabel();
			int labelIndex = labels.indexOf(label);
			
			if(labelIndex < 3){
				sent.setParagraphLabel(paragraphLabels[0]);
			} else if(labelIndex > 8){
				sent.setParagraphLabel(paragraphLabels[2]);
			} else{
				sent.setParagraphLabel(paragraphLabels[1]);
			}	
		}
	}
	
}
