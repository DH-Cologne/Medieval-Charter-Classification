package dataClasses.diploma;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

import org.w3c.dom.*;

import dataClasses.label.DiplomaticLabel;
import dataClasses.label.DiplomaticParagraphLabel;
import dataClasses.sentence.AbstractSentence;
import helpers.ReaderWriter;
import preprocessing.Preprocessor;

/**
 * Die AbstractDiploma-Klasse ist die abstrakte Datenklasse für alle Diplome und stellt
 * Methoden bereit, auf die alle Diploma-Klassen zugreifen müssen. Diese Methoden ermöglichen u.a. die Erstellung und Verwaltung
 * der zur Urkunde gehörenden Satzobjekte durch die erbenden Klassen.
 * Die Superklasse AbstractDiploma ermöglicht es, Methoden zu schreiben, die sowohl mit Objekten der Klasse Diploma
 * als auch der Klasse TrainingDiploma arbeiten können.
 * @author Alina Ostrowski
 *
 * @param <T> Die zu spezifizierende Satzklasse vom Typ AbstractSentence, mit der die erbenden Klassen ausschließlich arbeiten sollen. 
 */
public abstract class AbstractDiploma<T extends AbstractSentence> {

	// Soll die Urkunde genutzt werden (true) oder ist sie unnutzbar (false)?
	private boolean useDiploma;
	
	// Allgemeine Eigenschaften der Urkunde
	protected File file;
	protected String fileName;
	protected Document parsedXML;
	protected Node tenorNode;
	private String tenor;

	// Namen der möglichen Satzlabel
	protected List<String> paragraphLabelNames;
	protected List<String> partLabelNames;
	
	// In der Urkunde enthaltene Sätze sowie Informationen über diese
	protected List<T> sentences;
	protected int sentCount;
	protected int totalWordCount;

	// Regexes, die zur Satzerkennung genutzt werden
	
	/**
	 * Regex zur initialen Erkennung von Satzgrenzen. Der Volltext einer Urkunde wird hierdurch an allen Satzzeichen (,.;:?!-) gesplittet
	 * sowie an schließenden Klammern.
	 */
	protected String initialDelimiter = "(?<=\\))|((?<=[,\\.!\\?\\-;:]+)(?!\\)))";
	private List<String[]> abbreviationPairs;
	private List<String[]> paranthesisAnnotations;
	
	protected Preprocessor pp;
	
	public AbstractDiploma(String fileName, File file, Preprocessor pp){
		
		this.pp = pp;
		abbreviationPairs = pp.getAbbreviationPairs();
		paranthesisAnnotations = pp.getParanthesisAnnotations();
		
		this.fileName = fileName;
		this.file = file;

		// Ursprungsdatei parsen und so ein durchsuch- und manipulierbares Document-Objekt erzeugen
		parsedXML = ReaderWriter.parseFile(this.file);
		if(parsedXML == null){
			useDiploma = false;
			return;
		}
		
		tenor = ReaderWriter.extractUniqueElementText(parsedXML, "cei:tenor", true);
		tenorNode = initializeTenorNode();
		
		// testen, ob die Urkunde für die Klassifikation / das Training genutzt werden kann
		if(!testQualification()){
			useDiploma = false;
			return;
		}

		initializeLabelNames();
		
		// Sätze erkennen und entsprechende Objekte erstellen
		this.sentences = createSentences();
		if(sentences.size() == 0){
			useDiploma = false;
			return;
		}
		this.sentCount = sentences.size();
		
		// Positionsinformationen für alle Sätze setzen
		totalWordCount = 0;
		for(T sent : sentences){
			totalWordCount += sent.getTokens().size();
		}
		int wordCount = 0;
		for(int i = 0; i < sentences.size(); i++){
			T sent = sentences.get(i);
			sent.setIndexOfFirstWord(wordCount+1);
			sent.setInversedIndexOfFirstWord(totalWordCount - wordCount+1);
			wordCount+= sent.getTokens().size();
			sent.setIndexOfLastWord(wordCount);
			sent.setInversedIndexOfLastWord(totalWordCount - wordCount);
			sent.setWordRelativeIndex((double) wordCount / totalWordCount);
			sent.setIndex(i);
		}
		
		useDiploma = true;
		
	}
	
	/**
	 * Initialisierung der Listen mit den Labelnamen.
	 */
	private void initializeLabelNames() {
		paragraphLabelNames = new ArrayList<>();
		for(DiplomaticParagraphLabel label : DiplomaticParagraphLabel.values()){
			paragraphLabelNames.add(label.name());
		}
		if(this.fileName.equals("1189_V_18.cei.xml")) {
			System.out.println("1189");
		}
		partLabelNames = new ArrayList<>();
		for(DiplomaticLabel label : DiplomaticLabel.values()){
			partLabelNames.add(label.name());
		}
	}
	
	/**
	 * Testet, ob die Urkunde gewissen Anforderungen entspricht. Falls ja, gibt die Methode true zurück, falls nein gibt sie false zurück.
	 * Die Anforderungen sind:
	 * <ul>
	 * <li> Die Urkunde muss ein tenor-Element, d.h. einen Volltext enthalten, der nicht null, leer oder kürzer als 500 Zeichen sein darf.</li>
	 * <li> Der Text der Urkunde muss auf Latein geschrieben sein.</li>
	 * </ul>
	 * @return true, wenn die Urkunde den Anforderungen entspricht, sonst false.
	 */
	private boolean testQualification() {
		
		// Testen, ob die Urkunde auf Latein ist
		String lang = ReaderWriter.extractUniqueElementText(parsedXML, "cei:lang_MOM", true);
		String lowLang = lang.toLowerCase();
		if(!(lowLang.equals("latein") || lowLang.equals("lat.") || lowLang.equals("lat") || lowLang.equals("latin"))){
			System.out.println("The file "+fileName+" has the wrong language or no 'cei:lang_MOM'-Element. The language must be 'Latein'.");
			return false;
		}
		
		// Testen, ob ein tenor (Volltext) existiert und nicht leer bzw. auch umfangreich genug ist
		if(tenor == null || tenor.isEmpty() || tenor.length() < 500){ 
					
			System.out.println("The tenor element of the file "+fileName+" is empty or too short. The tenor must contain the full text transcription of the diploma.");
			return false;
		}
		
		return true;
		
	}
	
	/**
	 * Überprüft, ob eine String mit einer Abkürzung endet und falls ja, welcher Art sie ist.
	 * @param string Der zu überprüfende String.
	 * @return 1 falls die Abkürzung meist am Satzende auftritt, -1 falls die Abkürzung meist im Satzinneren auftritt,
	 * 		0 falls der Satz nicht mit einer Abkürzung endet.
	 */
	protected int endsWithAbbType(String string) {
		
		for(String[] pair : abbreviationPairs){
			// überprüfen, ob der String mit einer Abkürzung endet
			if(string.matches("[\\s\\S]*" + pair[0])){
				// überprüfen, ob es sich um eine typische Satzende-Abkürzung handelt
				if(Boolean.valueOf(pair[2])){
					return 1;
				} else {
				return -1;
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * Prüft, ob der übergebene String mit einer Annotation in Klammern endet, die
	 * keine editorische sondern eine inhaltliche Information enthält. Bsp.: "(C.)"
	 * für das Chrismon-Zeichen. Editorische Anmerkungen hingegen wären z.B. "(u auf Rasur)",
	 * "(schwer lesbar)" etc. und führen zu false.
	 * @param string Der zu prüfende String.
	 * @return true, wenn der String mit einer Annotation endet; sonst false.
	 */
	protected boolean endsWithEditorialAnnotation(String string) {
		for(String[] pair : paranthesisAnnotations){
			if(string.matches("[\\s\\S]*" + pair[0])){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Liest den übergebenen XML-Node ein und ezeugt eine Liste mit Listen, die die zu den Klassifikations-Sätzen gehörenden Childnodes enthalten.
	 * Jeder Satz besteht dabei aus einem oder mehreren #text-Knoten und gegebenenfalls weiteren Nicht-Textknoten. Der erste
	 * Knoten einer jeden ChildNodes-Liste ist ein #text-Knoten, der angibt, ob der vorherige Satz ein tatsächliches Satzende ist (".") oder nicht (""). 
	 * @param xml Eine XML-Datei, innerhalb derer die zurückzugebenden Knoten angelegt werden sollen.
	 * @param startNode Der Knoten, dessen Kindknoten auf die Klassifikations-Sätze verteilt werden sollen.
	 * @return Eine Liste mit mehreren Listen, die jeweils die Knoten eines Satzes enthalten.
	 */
	protected List<List<Node>> getSentencesByNodes(Document xml, Node startNode){
	
		List<List<Node>> sentenceNodes = new ArrayList<>();
		sentenceNodes.add(new ArrayList<Node>());
		NodeList childNodes = startNode.getChildNodes();

		// Variablen für die Abfrage des Zustandes der Satzerkennung:
		// Ist der zuletzt eingelesene Satz bereits vollständig, d.h. endet mit .,;:-?! oder einer eigenständigen Annotation (z.B. "(C.)")?
		boolean lastSentClosed = false;
		// Ist der letzte geschlossene Satz ein Satzende, d.h. endet nicht mit ,- oder einer Abkürzung (z.B. "MDCXIII.") oder uneigenständigen Annotation (z.B. "(schwer lesbar)" oder endet mit gar keinem Satzzeichen? 
		boolean lastSentEndsWithPoint = true;
		// Ist der letzte eingelesene Satz ein Satzende?
		boolean currentSentEndsWithPoint = true;
		
		// Alle Kindknoten des Startknotens überprüfen
		for(int i = 0; i < childNodes.getLength(); i++){
			Node currentNode = childNodes.item(i);
			
			// Bei Text-Knoten weitere Überprüfungen zur "Satztauglichkeit" vornehmen
			if(currentNode.getNodeName().equals("#text")){
				String textContent = currentNode.getTextContent();
				
				// Initiale Erkennung von möglichen Satzgrenzen. Der Volltext der Urkunde wird an allen Satzzeichen (",.;:?!-") gesplittet
				// sowie an schließenden Klammern ( ")" ). Die Delimiter werden an das Ende der so entstandenen Abschnitte angehängt.
				String[] sentences = textContent.trim().split(initialDelimiter);
				
				for(int j = 0; j < sentences.length; j++){
	
					// Aktueller, durch den initialen Delimiter erzeugter Abschnitt
					String currentSentence = sentences[j].trim();

					/*
					 * Test 1
					 * 
					 * Endet der aktuelle Abschnitt mit einer Abkürzung? Falls ja, dann hängt es vom Typ der Abkürzung ab, ob der Satz als
					 * eigenständig gilt und geschlossen werden kann (bei Abkürzungen, die i.d.R. ein Satzende markieren), oder ob der Satz
					 * noch ergänzt werden muss (bei Abkürzungen, die i.d.R. innerhalb des Satzes stehen). 
					 */
					int abbType = endsWithAbbType(currentSentence);
					if(abbType != 0){
						if(!lastSentClosed){
							updateLastSentenceNodesList(currentSentence, xml, sentenceNodes);
						} else{
						 	if(lastSentEndsWithPoint){
						 		updateLastSentenceNodesList(".", xml, sentenceNodes, 0);
							} else{
						 		updateLastSentenceNodesList("", xml, sentenceNodes, 0);
							}
							createNewSentenceNodesList(currentSentence, xml, sentenceNodes);
							lastSentEndsWithPoint = currentSentEndsWithPoint;
							
							
						}
						if(abbType > 0){
							lastSentClosed = true;
							currentSentEndsWithPoint = true;
						} else {
							lastSentClosed = false;
							currentSentEndsWithPoint = false;
						}
					}
					
					/*
					 * Test 2
					 * 
					 * Endet der aktuelle Abschnitt mit einem End-Satzzeichen (".!?;:")? Falls ja, dann handelt es sich um einen eigenständigen Satz
					 * und der Satz kann als abgeschlossen gelten. 
					 */
					else if(currentSentence.matches("[\\s\\S]*[.!?;:]")){
						if(!lastSentClosed){
							updateLastSentenceNodesList(currentSentence, xml, sentenceNodes);
						}else{
						 	if(lastSentEndsWithPoint){
						 		updateLastSentenceNodesList(".", xml, sentenceNodes, 0);
							} else{
						 		updateLastSentenceNodesList("", xml, sentenceNodes, 0);
							}
							createNewSentenceNodesList(currentSentence, xml, sentenceNodes);
							lastSentEndsWithPoint = currentSentEndsWithPoint;	
						}
						lastSentClosed = true;
						currentSentEndsWithPoint = true;
					}
					
					/*
					 * Test 3
					 * 
					 * Endet der aktuelle Abschnitt mit einem inneren Satzzeichen (",-")? Falls ja, dann hängt es von der Länge des Abschnitts ab,
					 * ob er als eigenständiger Satz gilt und geschlossen werden kann, oder ob er an den letzten bzw. nächsten Satz angefügt
					 * werden muss, da es sich vermutlich um eine Aufzählung handelt.
					 */
					else if(currentSentence.matches("[\\s\\S]*[,\\-]")){
						if(!lastSentClosed){
							updateLastSentenceNodesList(currentSentence, xml, sentenceNodes);
							lastSentClosed = true;
						}else{
							if(currentSentence.split("\\s+").length > 3){
								if(lastSentEndsWithPoint){
							 		updateLastSentenceNodesList(".", xml, sentenceNodes, 0);
								} else{
							 		updateLastSentenceNodesList("", xml, sentenceNodes, 0);
								}
								createNewSentenceNodesList(currentSentence, xml, sentenceNodes);
								lastSentEndsWithPoint = currentSentEndsWithPoint;
								lastSentClosed = true;
							} else {
								if(currentSentEndsWithPoint){
									if(lastSentEndsWithPoint){
								 		updateLastSentenceNodesList(".", xml, sentenceNodes, 0);
									} else{
								 		updateLastSentenceNodesList("", xml, sentenceNodes, 0);
									}
									createNewSentenceNodesList(currentSentence, xml, sentenceNodes);
									lastSentEndsWithPoint = currentSentEndsWithPoint;
									lastSentClosed = false;
								} else{
									updateLastSentenceNodesList(currentSentence, xml, sentenceNodes);
								}
							}
						}
						currentSentEndsWithPoint = false;
					}
					
					/*
					 * Test 4
					 * 
					 * Endet der aktuelle Abschnitt mit einer inhaltlichen Annotation (z.B. "(C.)" für "chrismon")? Falls ja, dann gilt der
					 * aktuelle Abschnitt als schließendes Satzende, falls der vorherige Satz bereits mit einem End-Satzzeichen geschlossen wurde.
					 * Sonst wird er an den letzten oder nächsten Satz angehängt.
					 */
					else if(endsWithEditorialAnnotation(currentSentence)){
						if(currentSentEndsWithPoint){
							updateLastSentenceNodesList(currentSentence, xml, sentenceNodes);
							lastSentClosed = true;
							currentSentEndsWithPoint = true;
						} else {
							if(!lastSentClosed){
								updateLastSentenceNodesList(currentSentence, xml, sentenceNodes);
							} else{
							 	if(lastSentEndsWithPoint){
							 		updateLastSentenceNodesList(".", xml, sentenceNodes, 0);
								} else{
							 		updateLastSentenceNodesList("", xml, sentenceNodes, 0);
								}
								createNewSentenceNodesList(currentSentence, xml, sentenceNodes);
								lastSentEndsWithPoint = currentSentEndsWithPoint;	
							}
							lastSentClosed = false;
							currentSentEndsWithPoint = false;
						}
					}
					
					/*
					 * Default 5
					 * 
					 * Wenn alle vorherigen Bedingungen nicht zutrafen, dann folgt daraus, dass der Abschnitt weder mit einem Satzzeichen, noch mit der schließenden Klammer einer Annotation endet.
					 * Das heißt, dass der Satz entweder mit einer nicht eigenständigen Annotation (z.B. "(schwer zu lesen)") oder ganz ohne Delimiter endet. Letzteres kann passieren, wenn der
					 * Text durch einen weiteren Nicht-Text-Kindknoten des Startknotens unterbrochen wird (z.B. <sup>) oder der Startknoten endet, ohne dass der
					 * darin enthaltene Text mit einem Satzzeichen endet.
					 * In beiden Fällen soll der so entstandene Abschnitt einfach dem letzten oder nächsten Satz hinzugefügt werden und als nicht abgeschlossen gelten.
					 */
					else {
						if(!lastSentClosed){
							updateLastSentenceNodesList(currentSentence, xml, sentenceNodes);
						} else {
						 	if(lastSentEndsWithPoint){
						 		updateLastSentenceNodesList(".", xml, sentenceNodes, 0);
							} else{
						 		updateLastSentenceNodesList("", xml, sentenceNodes, 0);
							}
							createNewSentenceNodesList(currentSentence, xml, sentenceNodes);
							lastSentEndsWithPoint = currentSentEndsWithPoint;	
						}
						lastSentClosed = false;
						currentSentEndsWithPoint = false;
					}
				}
			}
			
			// wenn es kein Text-Node ist, kopiere den Knoten in das neue Dokument und füge den Knoten dem letzten Satz hinzu
			else { 
				Node newNode = xml.importNode(currentNode, true);
				sentenceNodes.get(sentenceNodes.size()-1).add(newNode);
			}
			
		}
		if(lastSentEndsWithPoint){
	 		updateLastSentenceNodesList(".", xml, sentenceNodes, 0);
		} else{
	 		updateLastSentenceNodesList("", xml, sentenceNodes, 0);
		}

		return sentenceNodes;
	}

	/**
	 * Fügt der letzten Liste an Satz-Nodes einen weiteren Text-Knoten mit dem Text des übergebenen Strings hinzu. Der Knoten
	 * wird am Index i eingefügt. Falls ein i-Wert übergeben wird, der in der bisherigen Liste nicht existiert (kleiner als 0 oder
	 * größer als die Größe der Liste), dann wird der Knoten am Ende der Liste eingefügt.
	 * @param string Der hinzuzufügende String.
	 * @param xml Das Document-Objekt, in dem der neue Knoten mit dem String erzeugt werden soll.
	 * @param sentenceNodes Die Liste an Satzknoten-Listen, deren letztes Element aktualisiert werden soll.
	 * @param i Der Index, an dem der String eingefügt werden soll.
	 */
	private void updateLastSentenceNodesList(String string, Document xml, List<List<Node>> sentenceNodes, int i) {
		Node newNode = xml.createTextNode(string);
		List<Node> lastNodeList = sentenceNodes.get(sentenceNodes.size()-1);
		
		if(i < 0 || i > lastNodeList.size()-1){
			lastNodeList.add(newNode);
		} else {
			lastNodeList.add(i, newNode);
		}
	}
	
	/**
	 * Fügt der Liste an Satz-Node-Listen eine weitere Liste hinzu. Diese Liste erhält als erstes Element einen #text-Knoten
	 * mit dem übergebenen String als Text-Value.
	 * @param string Der initiale String der neuen Liste.
	 * @param xml Das Document-Objekt, in dem der neue Knoten mit dem String erzeugt werden soll.
	 * @param sentenceNodes Die Liste an Satzknoten-Listen, die erweitert werden soll.
	 */
	private void createNewSentenceNodesList(String string, Document xml, List<List<Node>> sentenceNodes) {

		List<Node> newList = new ArrayList<>();
		Node newNode = xml.createTextNode(string);
		newList.add(newNode);
		sentenceNodes.add(newList);
	}

	/**
	 * Fügt der letzten Liste an Satz-Nodes einen weiteren Text-Knoten mit dem Text des übergebenen Strings hinzu. Diese
	 * Methode ist eine Convenience-Methode und wirkt genau so, als würde man der Methode {@link #updateLastSentenceNodesList(String, Document, List, int)}
	 * als Parameter int i den Wert -1 übergeben.
	 * @param string Der hinzuzufügende String.
	 * @param xml Das Document-Objekt, in dem der neue Knoten mit dem String erzeugt werden soll.
	 * @param sentenceNodes Die Liste an Satzknoten-Listen, deren letztes Element aktualisiert werden soll.
	 */
	private void updateLastSentenceNodesList(String string, Document xml, List<List<Node>> sentenceNodes) {
		updateLastSentenceNodesList(string, xml, sentenceNodes, -1);
	}


	/**
	 * Testet, ob der Text des übergebenen Satzes
	 * nach der Text-Normalisierung nicht leer ist. 
	 * @param sentence
	 * @return false, wenn der normalisierte Text leer ist oder keine Buchstaben [a-zA-Z] enthält oder wenn die Satz-Tokens leer sind; sonst true.
	 */
	protected boolean trySentence(T sentence){
		pp.prepareSentence(sentence);
		String text = sentence.getText();
		if(text.matches("[^a-zA-Z]+") || text.isEmpty() || sentence.getTokens().isEmpty()){
			return false;
		}
		
		return true;
	}
	
	/**
	 * Erzeugt einen String, der den Textinhalt aller #text-Knoten der übergebenen Liste enthält.
	 * @param nodeList Die Liste mit Knoten, die in einen String überführt werden sollen.
	 * @return String mit dem Text der Knoten.
	 */
	protected String textNodesToOneString(List<Node> nodeList) {
		String fullText = "";
		
		for(int i = 0; i < nodeList.size(); i++){
			Node node = nodeList.get(i);
			if(node.getNodeName().equals("#text")){
				fullText+=" "+node.getTextContent();
			}
		}
		
		return fullText.trim();
	}

	public int getSentCount() {
		return sentCount;
	}
	
	public boolean useDiploma(){
		return useDiploma;
	}

	public String getFileName() {
		return fileName;
	}

	public List<T> getSentences() {
		return sentences;
	}
	
	public String getTenor() {
		return tenor;
	}

	public void setTenor(String tenor) {
		this.tenor = tenor;
	}
	
	public Node getTenorNode() {
		return tenorNode;
	}
	
	public int getTotalWordCount() {
		return totalWordCount;
	}
	
	/**
	 * Muss den tenor-Knoten des XML-Document-Objekts der Klasse zurückgeben.
	 * @return Den tenor-Knoten.
	 */
	protected abstract Node initializeTenorNode();
	
	/**
	 * Erstellt und gibt alle Sätze dieser Urkunde zurück. Alle Sätze werden dabei durch Preprocessor.prepareSentence() vorbereitet.
	 * @return Die Satz-Objekte der Urkunde.
	 */
	protected abstract List<T> createSentences();
}
