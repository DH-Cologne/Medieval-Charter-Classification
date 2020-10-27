package dataClasses.diploma;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import dataClasses.label.DiplomaticLabel;
import dataClasses.label.DiplomaticParagraphLabel;
import dataClasses.sentence.Sentence;
import helpers.ReaderWriter;
import preprocessing.Preprocessor;
/**
 * Datenklasse f�r zu klassifizierende Urkunden. Die Klasse arbeitet nur mit Sentence-Objekten.
 * Um zu funktionieren, muss ihr zum Einlesen der S�tze eine noch nicht ausgezeichnete
 * XML-Datei �bergeben werden, d.h. der Tenor darf keine Label- oder Paragraphen-Tags enthalten, sondern nur den Reintext.
 * 
 * @author Alina Ostrowski
 *
 */
public class Diploma extends AbstractDiploma<Sentence> {

	/**
	 * Das XML-Document-Objekt, das sp�ter die Output-XML-Datei bilden soll.
	 */
	private Document labeledXML;
	private List<List<Node>> sentenceNodes;

	public Diploma(String fileName, File file, Preprocessor pp) {
		super(fileName, file, pp);
	}

	/**
	 * Erzeugt eine Kopie der XML-Datei dieser Urkunde, die um die Label der S�tze der Urkunden erg�nzt ist.
	 * @param newDirPath Der Ordner, in dem die neue XML-Datei gespeichert werden soll.
	 * @param rootPath Der �berordner, bis zu dem die Ordnerstruktur der Ursprungsdatei in den neuen Ordner kopiert werden soll.
	 * Wenn dieser Wert null ist, dann werden keine Ordnerstrukturen �bernommen, sondern die Datei wird direkt im newDirPath angelegt.
	 */
	public void generateLabeledXML(String newDirPath, String rootPath){
		
		Node newTenor = labeledXML.createElement("cei:tenor");
		
		DiplomaticParagraphLabel[] paragraphLabels = DiplomaticParagraphLabel.values();
		List<DiplomaticLabel> labels = Arrays.asList(DiplomaticLabel.values());
		List<Node> paragraphNodes = new ArrayList<>();
		List<Node> partNodes = new ArrayList<>();		
		
		// Knoten f�r alle Paragraphenlabel erstellen
		for(DiplomaticParagraphLabel paragraph : paragraphLabels){
			String paragraphTagName = "cei:"+paragraph.toString();
			Node paragraphNode = labeledXML.createElement(paragraphTagName);
			paragraphNodes.add(paragraphNode);
		}

		// Knoten f�r alle Partlabel erstellen
		for(DiplomaticLabel label : labels){
			String paragraphTagName = "cei:"+label.toString();
			Node partNode = labeledXML.createElement(paragraphTagName);
			partNodes.add(partNode);
		}
		
		// Alle sentenceNodes eines jeden Satzes dem richtigen Partlabel-Knoten hinzuf�gen
		for(int i = 0; i < sentences.size(); i++){		
			
			DiplomaticLabel sentLabel = sentences.get(i).getLabel();
			Node partNode = partNodes.get(labels.indexOf(sentLabel));
			List<Node> nodeList = sentenceNodes.get(i);
			
			// ab 1 z�hlen, da an Index i der Punkt-Modifier steht
			for(int n = 1; n < nodeList.size(); n++){
				Node node = nodeList.get(n);
				if(node.getNodeName().equals("#text")){
					String text = " " + node.getNodeValue();
					node.setNodeValue(text);
				}
				partNode.appendChild(node);
			}
		}
		
		// Alle Partlabel-Knoten dem richtigen Paragraphenlabel-Knoten hinzuf�gen
		for(int i = 0; i < partNodes.size(); i++){
			Node currentNode = partNodes.get(i);
			if(!currentNode.hasChildNodes()) continue;
			
			if(i < 3) paragraphNodes.get(0).appendChild(currentNode);
			else if(i > 8) paragraphNodes.get(2).appendChild(currentNode);
			else paragraphNodes.get(1).appendChild(currentNode);
		}
		
		// Alle Paragraphen-Knoten dem Tenor-Knoten hinzuf�gen und 
		for(Node paragraphNode : paragraphNodes){
			newTenor.appendChild(paragraphNode);
		}
		
		Node tenorParent = ReaderWriter.getUniqueElementNode(labeledXML, "cei:body");
		
		tenorParent.replaceChild(newTenor, super.getTenorNode());
		
		String fullNewDirPath = null;
		if(rootPath != null){
			System.out.println("...copying directory structure from source directory to target directory...");
			// Wenn die Ordnerstruktur der Ursprungsdatei �bernommen werden soll, dann zun�chst
			// diese Ordnerstruktur anlegen
			fullNewDirPath = ReaderWriter.createDirectories(rootPath, newDirPath, this.file);
		}
		if(fullNewDirPath == null){
			fullNewDirPath = newDirPath;
		}
		
		// Das erstellte XML-Document-Objekt in eine tats�chliche Datei �bertragen
		ReaderWriter.writeXML(labeledXML, fullNewDirPath+"/"+fileName);
			
	}

	@Override
	protected Node initializeTenorNode() {
		labeledXML = ReaderWriter.copyDOMObject(parsedXML);
		return ReaderWriter.getUniqueElementNode(labeledXML, "cei:tenor");
	}

	@Override
	protected List<Sentence> createSentences() {
		List<List<Node>> sentenceNodes = getSentencesByNodes(labeledXML, tenorNode);
		List<Sentence> sentences = new ArrayList<>();
		
		for(int i = 0; i < sentenceNodes.size() ; i++){
			List<Node> nodeList = sentenceNodes.get(i);
			String text = textNodesToOneString(nodeList);
			if(text.length()>0){
				Sentence sentence = new Sentence(this, text);
				if(trySentence(sentence) == true){
					sentences.add(sentence);
				} else {
					// Wenn der Satz nicht genutzt werden soll, m�ssen die dazugeh�rigen Sentence-Nodes
					// dennoch beibehalten werden, um die sp�tere Ausgabe-XML nicht zu verf�lschen.
					// Die Satzknoten werden der letzten Knoten-Liste hinzugef�gt. Falls die Satzknoten selbst zur ersten
					// Liste geh�ren, werden sie der n�chsten Liste hinzugef�gt. Falls die Satzknoten die einzige Liste sind,
					// so geschieht nichts und die Funktion gibt im Zweifelsfall eine leere Sentence-Liste zur�ck.
					if(i>0){
						sentenceNodes.get(i-1).addAll(nodeList.subList(1, nodeList.size()));
					} else if(i+1 < sentenceNodes.size()) {
						List<Node> newList = sentenceNodes.get(i+1);
						newList.addAll(1, nodeList.subList(1, nodeList.size()));
						
						// Punkt-Modifier der aktuellen NodeList zur aufnehmenden NodeList hinzuf�gen
						newList.remove(0);
						newList.add(0, nodeList.get(0));
					}
					sentenceNodes.remove(i);
					i--;
				}
			}
		}
		
		this.sentenceNodes = sentenceNodes;
		return sentences;
	}
}
