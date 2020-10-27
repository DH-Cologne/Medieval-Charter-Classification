package helpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * Die ReaderWriter-Klasse stellt Methoden zur Verarbeitung von XML-Dateien (sowohl lesen als auch schreiben)
 * sowie zum Einlesen von Text-Dateien zur Verf�gung.
 * @author Alina Ostrowski
 *
 */
public class ReaderWriter {
	
	/*
	 * Feldvariablen f�r die Prozessierung von XML-Dateien. Die hier genutzten Klassen stammen
	 * aus den Java-eigenen Klassen zur XML-Prozessierung 
	 */
	private static DocumentBuilderFactory factory;
	private static DocumentBuilder builder = initializeParser();
	private static TransformerFactory tfFactory;
    private static Transformer transformer = initializeTransformer();

	private static DocumentBuilder initializeParser(){
		factory = DocumentBuilderFactory.newInstance();
		
		try {
			return factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			System.out.println("Problem with parser occured. Threw exception:");
			e.printStackTrace();
			return null;
		}
	}
	
	private static Transformer initializeTransformer(){
		tfFactory = TransformerFactory.newInstance();
		try {
			return tfFactory.newTransformer();
		} catch (TransformerConfigurationException e) {
			System.out.println("Problem with transformer occured. Threw exception:");
			e.printStackTrace();
			return null;
		}
		
	}
	
	/**
	 * Liest die �bergebene XML-Datei aus und �berf�hrt sie in ein Document-Objekt, das die geparste XML enth�lt.
	 * @param file Datei, die ausgelesen werden soll.
	 * @return Die �bergebene Datei als Document-Objekt oder null, falls ein Problem auftritt.
	 */
	public static Document parseFile(File file) {
		
		if(builder == null){
			System.out.println("The parser doesn't work. The called method isn't executed.");
			return null;
		}
		
		String fileName = file.getName();		
		Document XML;
		
		try {
			XML = builder.parse(file);
		} catch (SAXException e) {
			System.out.println("Problem with parser occured. Couldn't parse "+fileName+".Threw exception:");
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			System.out.println("The given file "+fileName+" could not be read by the parser. Threw exception:");
			e.printStackTrace();
			return null;
		}	
		return XML;
	}
	
	/**
	 * Erzeugt ein neues Document-Objekt mit demselben Inhalt wie das �bergebene original Document-Objekt.
	 * @param origDoc Das zu kopierende Document-Objekt.
	 * @return Eine unabh�ngige Kopie des origDoc.
	 */
	public static Document copyDOMObject(Document origDoc){
		
		if(builder == null){
			System.out.println("The parser doesn't work. The called method isn't executed.");
			return null;
		}
		
		Document newDoc = builder.newDocument();
		
		Node origRoot = origDoc.getDocumentElement();
        Node newRoot = newDoc.importNode(origRoot, true);
        
        newDoc.appendChild(newRoot);
        
        return newDoc;
	}
	
	/**
	 * Gibt dasjenige einzigartige Element zur�ck, welches den �bergebenen Namen tr�gt. Genauer gesagt:
	 * Gibt das erste aller Elemente zur�ck, welche den �bergebenen Namen tragen.
	 * @param XML Das XML-Document Objekt, in dem das Element gesucht werden soll.
	 * @param elementName Der Name des Elements, welches gesucht ist.
	 * @return Das gesuchte Element oder null, falls in der XML kein Element dieses Namens existiert.
	 */
	public static Node getUniqueElementNode(Document XML, String elementName) {
		
		NodeList nodes = XML.getElementsByTagName(elementName);
		if(nodes.getLength() == 0){
			return null;
		}
		return XML.getElementsByTagName(elementName).item(0);
	}
	
	/**
	 * Gibt den Text desjenigen einzigartigen Elements sowie seiner Kindelemente zur�ck, welches den �bergebenen Namen tr�gt. Genauer gesagt:
	 * Gibt den Text des ersten aller Elemente zur�ck, welche den �bergebenen Namen tragen. Wenn trimWhitespaces = true gew�hlt wird,
	 * dann werden f�hrende und abschlie�ende Whitespaces entfernt sowie vielfache Whitespaces zu einem einzigen Whitespace verschmolzen.
	 * @param XML Das XML-Document-Objekt, aus dem der Text gezogen werden soll. 
	 * @param elementName Der Name des Elementes, dessen Text gesucht wird.
	 * @param trimWhitspaces Angabe, ob Whitespaces entfernt bzw. vereinfacht werden sollen.
	 * @return Den Text des Elements mit dem �bergebenen Namen oder null, falls dieses Element nicht existiert.
	 */
	public static String extractUniqueElementText(Document XML, String elementName, boolean trimWhitspaces) {
		NodeList nodes = XML.getElementsByTagName(elementName);
		if(nodes.getLength() == 0){
			return null;
		}
		Node node = nodes.item(0);
		
		String text = node.getTextContent();
		
		// f�hrende und nachgestellte whitespaces entfernen
		if(trimWhitspaces){
			text.trim();
			text.replaceAll("\\s", " ");
		}
		
		return text;
	}
	
	/**
	 * Gibt alle Attribute des ersten aller Elemente mit dem �bergebenen Element-Namen zur�ck.
	 * @param XML Das XML-Document-Objekt, aus dem die Attribute extrahiert werden sollen.
	 * @param elementName Der Name des Elements, dessen Attribute gesucht sind.
	 * @return Eine Map mit dem Namen eines Attribut-Elements als Key und dem Text-Inhalt dieses Attributs als Value.
	 */
	public static Map<String, String> getAllAttributes(Document XML, String elementName){
		if(builder == null){
			System.out.println("The parser doesn't work. The called method isn't executed.");
			return null;
		}
		
		Map<String, String> attributeMap = new HashMap<>();
		
		Node node = getUniqueElementNode(XML, elementName);
		
		if(node != null && node.hasAttributes()){
			NamedNodeMap attributes = node.getAttributes();
			
			for(int i = 0; i < attributes.getLength(); i++){
				Node attribute = attributes.item(i);
				String name = attribute.getNodeName();
				String value = attribute.getNodeValue();
				attributeMap.put(name, value);
			}
		}
		
		return attributeMap;
		
	}
	
	/**
	 * Gibt den Textinhalt des Attributs mit dem �bergebenen Namen zur�ck, welches ein Attribut des ersten aller Elemente mit
	 * dem �bergebenen Elementnamen ist.
	 * @param XML Das XML-Document-Objekt, in dem das Attribut gesucht werden soll.
	 * @param elementName Das Element, welches das gesuchte Attribut enth�lt.
	 * @param attributeName Der Name des gesuchten Attributs.
	 * @return Den Textinhalt des gesuchten Attributs oder null, falls das Element oder das Attribut nicht existiert.
	 */
	public static String getAttributeValue(Document XML, String elementName, String attributeName){
		if(builder == null){
			System.out.println("The parser doesn't work. The called method isn't executed.");
			return null;
		}
				
		Node node = getUniqueElementNode(XML, elementName);
		
		if(node!= null && !node.hasAttributes()){
		
			NamedNodeMap attributes = node.getAttributes();
			
			for(int i = 0; i < attributes.getLength(); i++){
				Node attribute = attributes.item(i);
				String name = attribute.getNodeName();
				if(name.equals(attributeName)){
					return attribute.getNodeValue();
				}
			}
		}
		
		return null;
	}
	
	/**
	 * �berpr�ft, ob die Elternordner einer Datei existieren und legt sie - falls sie nicht existieren - im �bergebenen neuen Ordner als Unterordner an.
	 * Diese �berpr�fung wird durchgef�hrt, bis der n�chste �berordner der �bergebene root-Ordner ist.
	 * @param rootPath Der Ordner, bis zu dem die alte Ordnerstruktur kopiert werden soll.
	 * @param newDirPath Der Ordner, in dem die neue Ordnerstruktur angelegt werden soll.
	 * @param file Die Datei, deren �bergeordnete Ordnerstruktur kopiert werden soll.
	 * @return Den um die �berordner der file erg�nzten newDirPath als String.
	 */
	public static String createDirectories(String rootPath, String newDirPath, File file){
		File rootDir = new File(rootPath);
		
		// alle �bergeordneten Ordner bis zum rootPath finden
		List<String> pathParts = new ArrayList<>();	
		File nextParent = file.getParentFile();
		while(!nextParent.equals(rootDir)){
			pathParts.add(nextParent.getName());
			nextParent = nextParent.getParentFile();
		}
		
		// f�r alle gefundenen �berordner pr�fen, ob sie existieren, und - falls nicht - neu anlegen
		for(int i = (pathParts.size()-1); i >=0; i--){
			// dem neuen Pfad die Pfadabschnitte der neuen Unterordner hinzuf�gen
			newDirPath += "/" + pathParts.get(i);
			File parentDir = new File(newDirPath);
			if(!parentDir.exists()){
				try{
					if(parentDir.mkdir()) {
						System.out.println("Created directory "+parentDir.getPath());
					} else {
			    		System.out.println("Couldn't copy file '"+file.getName()+"'because of directory fail.");
			    		System.out.println("Tried to create directory "+parentDir.getPath());
			    		return null;
					}
				} catch(Exception e3){
		    		System.out.println("Couldn't copy file '"+file.getName()+"'because of directory fail.");
					e3.printStackTrace();
					return null;
				}
			}
		}
		
		return newDirPath;
	}
	
	/**
	 * Nutzt die Java-eigene Transformer-Klasse, um ein virtuelles XML-Document-Objekt in eine tats�chlich existierende XML-Datei zu verwandeln.
	 * @param XML Das XML-Document-Objekt, f�r welches eine Datei geschrieben werden soll.
	 * @param path Die Datei, in der die XML gespeichert werden soll.
	 * @return true, wenn die Speicherung erfolgreich war; sonst false.
	 */
	public static boolean writeXML(Document XML, String path) {
		
		if(transformer == null){
			System.out.println("The parser doesn't work. The called method isn't executed.");
			return false;
		}
		
		File file = new File(path);
				
		if(!file.exists()){
			try {
				file.createNewFile();
			} catch (IOException e) {
				System.out.println("Couldn't write DOM Object to file "+path+". Threw exception:");
				e.printStackTrace();
				return false;
			}
		}
		
        DOMSource domSource = new DOMSource(XML);
        StreamResult streamResult = new StreamResult(file);
        try {
			transformer.transform(domSource, streamResult);
			return true;
		} catch (TransformerException e) {
			System.out.println("Couldn't write DOM Object to file "+path+". Threw exception:");
			e.printStackTrace();
			return false;
		}
		
	}
	
	/**
	 * Liest eine Text-Datei Zeile f�r Zeile aus und gibt eine Liste mit allen ausgelesenen Zeilen zur�ck.
	 * @param file Die auszulesende Datei.
	 * @param trim Angabe, ob die gelesenen Zeilen getrimmt werden sollen.
	 * @return Eine Liste mit allen ausgelesenen Zeilen als String.
	 * @throws IOException
	 */
	public static List<String> readLineByLine(File file, boolean trim) throws IOException{
		List<String> lines = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(file))){
			String line = br.readLine();
			while(line != null){
				if(trim){
					line.trim();
				}
				lines.add(line);
				line = br.readLine();
			}
		}
			
			return lines;
	}
	
	/**
	 * Liest eine CSV-Datei zeilenweise ein und splittet die Zeilen an dem �bergebenen Delimiter.
	 * @param file Die zu lesende Datei.
	 * @param trim boolean, der angibt, ob die einzelnen Zeilen-Bestandteile nach dem Aufsplitten von
	 * 			f�hrenden und schlie�enden Whitespaces bereinigt werden sollen.
	 * @param delimiter Regex, an dem die einzelnen Zeilen gesplittet werden sollen.
	 * @param length Legt fest, wie viele Values die zur�ckgegebenen Arrays enthalten sollen.
	 * 			Ist ein Array k�rzer als length, wird er nicht zur�ckgegeben. Ist er l�nger,
	 * 			wird er am Index length abgeschnitten. Wird length=0 �bergeben, werden alle
	 * 			Arrays unabh�ngig von ihrer L�nge zur�ckgegeben. 
	 * @return Eine Liste, die pro Zeile der CSV-Datei ein Array mit deren Values enth�lt. Die Liste kann leer sein, wenn
	 * beim Einlesen eine Exception auftritt, sie ist jedoch nie null. 
	 */
	public static List<String[]> readCSV(File file, boolean trim, String delimiter, int length){
		List<String> lines = new ArrayList<>();
		List<String[]> arrays = new ArrayList<>();
		
		try(BufferedReader br = new BufferedReader(new FileReader(file))){
			String line = br.readLine();
			while(line != null){
				line.trim();
				lines.add(line);
				line = br.readLine();
			}
		} catch (IOException e) {
			System.out.println("CSV file "+file.getName()+" couldn't be read.");
			e.printStackTrace();
			return arrays;
		}
		
		// Zeilen anhand des �bergebenen Delimiters in einen Array aufsplitten und (falls gewollt) trimmen
		for(String line : lines){
			String[] array = line.split(delimiter);
			if(length != 0){
				if(array.length < length){
					continue;
				} else if(array.length > length){
					String[] newArray = new String[length];
					for(int i = 0; i < length; i++){
						newArray[i] = array[i];
					}
					arrays.add(newArray);
					continue;
				}
			}
			if(trim){
				for(int i = 0; i < array.length; i++){
					array[i] = array[i].trim();
				}
			}
			arrays.add(array);
		}
		
		return arrays;
	}

	/**
	 * Schreibt jeden String der �bergebenen Liste in eine Text-Datei.
	 * @param lines Die zu schreibenden Strings als Liste.
	 * @param filePath Die Datei, in die der Text geschrieben werden soll.
	 */
	public static void writeLineByLineToFile(List<String> lines, String filePath) {
		
		File file = new File(filePath);
		if(!file.exists()){
			try {
				file.createNewFile();
			} catch (IOException e) {
				System.out.println("Couldn't create File '"+filePath+"' for writing text line by line. Text is not written.");
				e.printStackTrace();
			}
		}
		
		try(BufferedWriter br = new BufferedWriter(new FileWriter(filePath))) {
			for(String line : lines){
				br.write(line);
				br.newLine();
			}
		} catch (IOException e) {
			System.out.println("Problem with writing text line by line to the file with path '"+filePath+"'. The text is not written.");
			e.printStackTrace();
		}
		
	}
}
