package helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;

/**
 * Diese Hilfsklasse (bzw. Hilfsprogramm) habe ich benutzt, um die ursprünglichen über 600.000 Urkunden
 * von Monasterium auf ihre Verwendbarkeit hin zu überprüfen und unnützliche Dateien auszusortieren.
 * Im DiplomeAnalyzer-Programm selbst wird die Klasse nicht verwendet, sie nutzt jedoch Methoden
 * aus der ReaderWriter-Klasse.
 * @author Alina Ostrowski
 *
 */
public class MomFilter {


	/**
	 * Der Ordner, in dem die zu filternden Dateien liegen
	 */
	private static String originPath = "data/MonasteriumUrkunden";
	/**
	 * Der Ordner, in den die verwendbaren Dateien kopiert werden sollen
	 */
	private static String goodPath = "data/originalXMLdocs";
	/**
	 * Der Ordner, in den die nicht nutzbaren Dateien kopiert werden sollen
	 */
	private static String trashPath = "data/unusedMOM";
	private static Set<String> trashDirectories;
	/**
	 * Eine Textdatei, in der alle Namen derjenigen Ordner stehen, die Daten von GoogleData enthalten
	 */
	private static String googleNames = "src/config.txts/googleNames.txt";
	private static List<String> googleDataNames;
	private static File origDataDir;
	
	//Variablen für eine spätere kleine Statistik zu entfernten und aufgenommenen Urkunden
	private static int wrongTenor = 0;
	private static int wrongLanguage = 0;
	private static int wrongDate = 0;
	private static int googleData = 0;
	private static int qualified = 0;
	
	
	public static void main(String[] args) {

		origDataDir = new File(originPath);
		googleDataNames = readGoogleNames();

		// Unterordner im Ordner der ausgesonderten Dateien anlegen
		trashDirectories = new HashSet<>();
		trashDirectories.add("googleData");
		trashDirectories.add("noKing");
		trashDirectories.add("noTenor");
		trashDirectories.add("noLatin");
		trashDirectories.add("outOfDate");
		trashDirectories.add("falseFileType");
		createTrashDirectories();
		
		sortFiles(origDataDir);
		int total = wrongLanguage + wrongTenor + wrongDate;
		System.out.println("Checked all files. Removed in total "+total+" files and "+googleData+" googleData directories from the set.");
		System.out.println("No tenor: "+wrongTenor+"; Wrong language: "+wrongLanguage+"; Wrong date: "+wrongDate+"; Google Data directories: "+googleData);
		System.out.println("Qualified in total "+qualified+" files for further calculations.");
	}
	
	/**
	 * Legt - falls noch nicht vorhanden - für jeden Namen aus dem trashDirectories-Set einen
	 * neuen Unterordner im Ordner für die ausgesonderten Dateien an.
	 */
	private static void createTrashDirectories() {
		for(String dirPath : trashDirectories){
			File dir = new File(trashPath+"/"+dirPath);
			dir.mkdir();
		}
		
		System.out.println("Created trash directories");
		
	}

	/**
	 * Liest die txt-Datei, welche die Namen der auszusondernden googleData-Ordner enthält, ein
	 * und gibt alle Namen als Liste zurück.
	 * @return eine Liste mit den Namen der googleData-Ordner
	 */
	private static List<String> readGoogleNames() {
		
		List<String> names = new ArrayList<>();
		try {
			names = ReaderWriter.readLineByLine(new File(googleNames), true);
		} catch (IOException e) {
			System.out.println("The file for googleNames couldn't be read.");
			e.printStackTrace();
		}
		return names;
	}

	/**
	 * Iteriert über alle Dateien des übergebenen File-Objekts sowie - falls dieses
	 * ein Ordner ist - über die Dateien in allen Unterordnern und entscheidet mithilfe
	 * der Funktion filterFile(), in welchen Ordner die Datei sortiert werden soll.
	 * @param startFile das File-Objekt, dessen Dateien sortiert werden sollen.
	 */
	private static void sortFiles(File startFile){

		File[] files = startFile.listFiles();
		for(File file : files){
			if(file.isDirectory()){
				// GoogleData-Ordner werden vorab komplett aussortiert
				if(googleDataNames.contains(file.getName())){

					googleData+=1;
					copyDirectory(file, trashPath+"/googleData");
					continue;
				}
				System.out.println("Reading directory "+file.getName());
				sortFiles(file);
			} else if(filterFile(file)){
				qualified+=1;
				copyFile(file, goodPath);
			}
				
		}
	}

	/**
	 * Kopiert einen Ordner, indem über alle darin enthaltenen File-Objekte
	 * iteriert wird und alle Datein mit copyFile(), alle Unterordner wiederum
	 * mit copyDirectory() kopiert werden.
	 * @param dir der zu kopierende Ordner
	 * @param newDirPath der Zielordner
	 */
	private static void copyDirectory(File dir, String newDirPath){
		File[] children = dir.listFiles();
		for(File child : children){
			if(child.isDirectory()){
				copyDirectory(child, newDirPath);
			} else {
				copyFile(child, newDirPath);
			}
		}
	}
	
	/**
	 * Kopiert die übergebene Datei in den genannten Zielordner. Alle darüberliegenden bis zur
	 * Ebene des originPath-Ordners reichende Ordnerstrukturen werden dabei in den Zielordner übernommen.
	 * @param file zu kopierende Datei
	 * @param newFilePath Zielordner
	 * @return true, wenn die Datei erfolgreich kopiert wurde, sonst false
	 */
	private static boolean copyFile(File file, String newFilePath){
		
		// Um die ursprüngliche Ordnerstruktur beizubehalten, wird die Pfadstruktur des Ursprungsordners
		// ermittelt, um sie in den Zielordner zu übertragen
		String newDirPath = ReaderWriter.createDirectories(origDataDir.getPath(), newFilePath, file);
		if(newDirPath == null){
			return false;
		}
		newDirPath+="/"+file.getName();

		// neue Datei im Zielordner anlegen
		File newFile = new File(newDirPath);
		
		try {
			newFile.createNewFile();
			
			// Inhalt der Ursprungsdatei mithilfe von Output- und InputStream in die Zieldatei kopieren 
		    try (InputStream is = new FileInputStream(file)){
		    	try(OutputStream os = new FileOutputStream(newFile)){
		    		byte[] buffer = new byte[1024];
			        int length;
			        while ((length = is.read(buffer)) > 0) {
			            os.write(buffer, 0, length);
			        }
		    	}
		    }
	    } catch (Exception e) {
    		System.out.println("Couldn't copy file '"+file.getName()+"'.");
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	/**
	 * Überprüft, ob die übergebene Datei folgenden Anforderungen entspricht:
	 * - richtiges Dateiformat
	 * - enthält eine Volltexttranskription (<cei:tenor>), die nicht leer oder zu kurz ist
	 * - ist als Lateinisch gekennzeichnet (<cei:lang_MOM>)
	 * - enthält to- und from- Attribute (<cei:dateRange>), die innerhalb der gewünschten Zeitspanne liegen
	 * - enthält im tenor eins der Wörter "rex", "regina", "imperator", "imperatrix" und ist darum wahrscheinlich eine Königsurkunde
	 * Entspricht die Datei einer dieser Anforderungen nicht, wird sie der Methode copyFile() übergeben
	 * und in einen entsprechenden Unterordner des Zielordners für ausgesonderte Dateien kopiert. 
	 * @param file die zu überprüfende Datei
	 * @return true, wenn die Datei *allen* Anforderungen entspricht, sonst false 
	 */
	public static boolean filterFile(File file){

		// testen, ob es sich um eine cei.xml-Datei handelt
		if(!file.getName().endsWith(".cei.xml")){
			copyFile(file, trashPath+"/falseFileType");
			return false;
		}
		
		Document parsedXML = ReaderWriter.parseFile(file);
		
		// Testen, ob ein tenor existiert und nicht leer bzw. umfangreich genug ist
		String tenor = ReaderWriter.extractUniqueElementText(parsedXML, "cei:tenor", true);
		if(tenor == null || tenor.isEmpty() || tenor.length() < 1000) {
			wrongTenor+=1;
			copyFile(file, trashPath+"/noTenor");
			return false;
		}
		
		// Testen, ob die Urkunde auf Latein ist
		String lang = ReaderWriter.extractUniqueElementText(parsedXML, "cei:lang_MOM", true);
		if(lang != null){
			String lowLang = lang.toLowerCase();
			if(!(lowLang.equals("latein") || lowLang.equals("lat.") || lowLang.equals("lat") || lowLang.equals("latin"))){
				wrongLanguage+=1;
				copyFile(file, trashPath+"/noLatin");
				return false;
			}
		} else {
			wrongLanguage+=1;
			copyFile(file, trashPath+"/noLatin");
			return false;
		}
		
		// testen, ob die Urkunde aus der richtigen Zeit stammt: Alles zwischen dem 1.1.950 und dem 31.12.1399 wird akzeptiert
		Map<String, String> dateRange = ReaderWriter.getAllAttributes(parsedXML, "cei:dateRange");
		String fromString = dateRange.get("from");
		String toString = dateRange.get("to");
		int from = 0;
		int to = 0;
		if(!(fromString == null && toString == null)){
			from = convertDateStringToInt(fromString, 9500101);
			to = convertDateStringToInt(toString, 13991231);
		} else {
			wrongDate+=1;
			copyFile(file, trashPath+"/outOfDate");
			return false;
		}
		if(from < 9500101 || to > 13991231){
			wrongDate+=1;
			copyFile(file, trashPath+"/outOfDate");
			return false;
		}
		
		// testen, ob es sich um eine Königsurkunde handelt
		if(! (tenor.contains("rex") || tenor.contains("imperator") || tenor.contains("imperatrix") || tenor.contains("regina"))){
			wrongTenor+=1;
			copyFile(file, trashPath+"/noKing");
			return false;
		}
			

		return true;
		
	}

	/**
	 * Konvertiert den übergebenen String in einen int. Sollte das nicht möglich sein,
	 * gibt es den angegebenen Alternativwert zurück.
	 * @param string der zu konvertierende String
	 * @param i der Alternativwert, der im Falle einer Exception zurückgegeben werden soll
	 * @return den String als int oder den Alternativwert
	 */
	private static int convertDateStringToInt(String string, int i) {
		int date = i;
		if(string != null){
			try{
				date = Integer.parseInt(string);
			} catch(Exception e){
				System.out.println("Couldn't convert dateRange to int: "+string);
				e.printStackTrace();
			}
		}
		return date;
		
	}
	
}
