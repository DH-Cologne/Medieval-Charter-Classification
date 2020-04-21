package dataClasses.diploma;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import dataClasses.label.DiplomaticLabel;
import dataClasses.label.DiplomaticParagraphLabel;
import dataClasses.sentence.TrainingSentence;
import helpers.ReaderWriter;
import preprocessing.Preprocessor;

/**
 * Datenklasse für Trainingsurkunden. Die Klasse arbeitet nur mit TrainingSentence-Objekten.
 * Um zu funktionieren, muss ihr zum Einlesen der Sätze eine bereits vollständig ausgezeichnete
 * XML-Datei übergeben werden. 
 * 
 * @author Alina Ostrowski
 *
 */
public class TrainingDiploma extends AbstractDiploma<TrainingSentence> {

	
	public TrainingDiploma(String fileName, File file, Preprocessor pp) {
		super(fileName, file, pp);
	}
	
	@Override
	protected Node initializeTenorNode() {
		return ReaderWriter.getUniqueElementNode(parsedXML, "cei:tenor");
	}

	@Override
	protected List<TrainingSentence> createSentences() {
		List<TrainingSentence> sentences = new ArrayList<>();
		NodeList diplParagraphs = tenorNode.getChildNodes();
		
		// Name des Paragraphenlabels anhand der entsprechenden cei-Tags bestimmen
		for(int i = 0; i < diplParagraphs.getLength(); i++){
			Node paragraph = diplParagraphs.item(i);
			String nodeName = paragraph.getNodeName();
			if(nodeName.equals("#text"))continue;
			NodeList diplParts = paragraph.getChildNodes();
			String paragraphName = nodeName.substring(4);
			if(!paragraphLabelNames.contains(paragraphName)) continue;
			DiplomaticParagraphLabel paragraphLabel = DiplomaticParagraphLabel.valueOf(paragraphName);

			// Name des Partlabels anhand der entsprechenden cei-Tags bestimmen
			for(int j = 0; j < diplParts.getLength(); j++){

				Node part = diplParts.item(j);
				String nodeName2 = part.getNodeName();
				if(nodeName2.equals("#text"))continue;
				String partName = nodeName2.substring(4);
				if(!partLabelNames.contains(partName)) continue;
				DiplomaticLabel partLabel = DiplomaticLabel.valueOf(partName);
				// Satzknoten erzeugen
				List<List<Node>> sentenceNodes = getSentencesByNodes(parsedXML, part);
				// Satzknoten in Satz-Objekte überführen und der Gesamt-Satz-Liste hinzufügen
				List<TrainingSentence> sentsInPart = sentNodesToSentences(sentenceNodes, partLabel, paragraphLabel);					
				sentences.addAll(sentsInPart);
			}
		}
		
		return sentences;
	}
	
	/**
	 * Erzeugt für jede Liste der sentenceNodes-Liste ein Satzobjekt, dessen Text dem konkatenierten Textinhalt aller #text-Knoten
	 * der Liste entspricht. Das gilt nur, solange der Text nach der Normalisierung nicht leer ist. Ist das der Fall, dann wird der Satz nicht zurückgegeben.
	 * @param sentenceNodes Eine Liste mit Listen für alle Sätze bzw. deren Knoten.
	 * @param partLabel Das Partlabel der zu erzeugenden Sätze.
	 * @param paragraphLabel Das Paragraphlabel der zu erzeugenden Sätze.
	 * @return Alle erzeugten Satz-Objekte.
	 */
	private List<TrainingSentence> sentNodesToSentences(List<List<Node>> sentenceNodes, DiplomaticLabel partLabel, DiplomaticParagraphLabel paragraphLabel){
		List<TrainingSentence> sentences = new ArrayList<>();
		
		for(int i = 0; i < sentenceNodes.size() ; i++){
			List<Node> nodeList = sentenceNodes.get(i);
			String text = textNodesToOneString(nodeList);
			if(text.length()>0){
				TrainingSentence sentence = new TrainingSentence(this, text, paragraphLabel, partLabel);
				if(trySentence(sentence) == true){
					sentences.add(sentence);
				}
			}
		}
		return sentences;
	}

}
