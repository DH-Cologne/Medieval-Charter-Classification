package app;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import classification.DiplomaticClassifier;
import classification.ProbabilisticClassifier;

import config.ClassificationConfig;
import config.Milestones;
import preprocessing.Preprocessor;

import dataClasses.diploma.AbstractDiploma;
import dataClasses.diploma.Diploma;
import dataClasses.diploma.TrainingDiploma;
import dataClasses.sentence.AbstractSentence;

/**
 * Main-Klasse des Programms. Durch die Ausführung der main-Methode kann die Evaluation oder
 * Klassifikation gestartet werden. Die Erstellung der Urkunden-Objekte sowie die einzelnen
 * Klassifikationsschritte werden hier eingeleitet.
 * @author Alina Ostrowski
 *
 */
public class DiplomaAnalyzerApp {
	
	// Variablen, die die Pfade zu den Dateien tragen
	private static String dirPath = "data/inputData";
	private static String newPath = "data/outputXMLdocs";
	private static String evalResultPath = "documentation/evaluationResults/";
	static String trainingPath = "data/testData";
	
	static String resolverPath = "src/config/txts/editorialSignsResolvers.txt";
	static String indicatorPath = "src/config/txts/diplomaticIndicators.txt";
	static String abbreviationPath = "src/config/txts/abbreviations.txt";
	static String capitalLetterPath = "src/config/txts/capitalLetterWords.txt";
	static String paranthesisPath = "src/config/txts/paranthesisAnnotations.txt";
	
	private static ClassificationConfig cc = new ClassificationConfig();
	private static Preprocessor pp;
	private static ProbabilisticClassifier pc;
	private static DiplomaticClassifier dc;
	private static Evaluation ev;
	
	public static void main(String[] args) {

		try(BufferedReader br = new BufferedReader(new InputStreamReader(System.in))){
			
			System.out.println("---!WELCOME!---");
			System.out.println("This is a Diploma Classifier that annotates the sentences of medieval diplomas corresponding to their diplomatic function.");
			System.out.println();
		
			System.out.println("Would you like to start a new evaluation or start the classifier for classifying one or more unclassified diplomas?");
			System.out.println("Answer with 'e' for evaluation and with 'c' for classifier.");
			String method = br.readLine().trim();
			while(!(method.equals("e") || method.equals("c"))){
				System.out.println("Please answer only with one of the following answers: 'e' or 'c'");
				method = br.readLine();
			}
			
			if(method.equals("e")){
				System.out.println("You chose evaluation. The evaluation will start now.");
				System.out.println();
				ev = new Evaluation(evalResultPath);
				ev.evaluate();
			} else{
				System.out.println("You chose classification. Please wait while the training data is initialized.");
				System.out.println();
				pp = new Preprocessor(resolverPath, abbreviationPath, capitalLetterPath, paranthesisPath);
				System.out.println("***INITIALIZING TRAINING DIPLOMAS***");
				List<TrainingDiploma> trainingData = initializeTrainingDiplomas(trainingPath, pp);
				System.out.println("Found "+trainingData.size()+" diplomas for training.");
				System.out.println();
				
				pc = new ProbabilisticClassifier(pp, trainingData, cc.getUseBigramsInsteadOfTokens(), cc.getVectorType());
				
				Milestones ms = new Milestones(trainingData, cc.getSequProbsTolerance());
				dc = new DiplomaticClassifier(indicatorPath, ms);
	
				System.out.println();
				System.out.println("The classifier is now ready for classification. You may choose between two modes:");
				System.out.println("STANDARD: The classifier uses the standard input directory (data/inputData) as source for the diplomas that are to be classified. It terminates after classification.");
				System.out.println("INTERACTIVE: Specify another directory or a single file by entering a path to a file/directory. The program terminates when you enter 'EXIT'.");
				System.out.println("\tInfo: By choosing 'STANDARD' you ensure that the directory structure of your diplomas is kept and transferred to the output directory.\n\tPlease note that this is not possible in the interactive mode!");
				System.out.println("Please choose now by entering 'STANDARD' or 'INTERACTIVE'!");
				
				String mode = br.readLine().trim();
				while(!(mode.equals("STANDARD") || mode.equals("INTERACTIVE"))){
					System.out.println("Please answer only with one of the following answers: 'STANDARD' or 'INTERACTIVE'");
					mode = br.readLine();
				}
				
				if(mode.equals("STANDARD")){
					System.out.println("You chose the standard mode. The classification will start now.");
					System.out.println();
					startApp(dirPath, true);
				} else{
					System.out.println("You chose the interactive mode. The classification will start after you've entered a valid path.");
					System.out.println();
					System.out.println("Please enter the path that leads to the directory or file that you want to classify!");
					System.out.println("Info: The path must be absolute or relative to the directory of this programm's source code.");
					System.out.println("Enter 'EXIT' to terminate the program.");
					System.out.println();
					
					String path = br.readLine().trim();
					while(!path.equals("EXIT")){
						System.out.println("The classification for the path '"+path+"' will start now:");
						System.out.println();
						startApp(path, false);
						System.out.println();
						System.out.println("Please enter a valid path to classify diplomas or enter 'EXIT' to terminate the program!");
						path = br.readLine().trim();
						
					}
				}
			}
		} catch (IOException e) {
			System.out.println("Sorry, there's been a problem with reading the commands from the console.");
			System.out.println("The program is cancelled.");
			e.printStackTrace();
		}

		System.out.println("***TERMINATED***");
	}

	/**
	 * Klassifiziert die Dateien des übergebenen Pfades und erzeugt gelabelte XML-Dateien.
	 * @param path Der Pfad, unter dem sich die zu klassifizierende/n Datei/en befindet/n.
	 * @param usePathAsRootDir Soll bei der Erstellung der neuen XML-Dateien die übergeordnete Ordnerstruktur einer Ursprungsdatei übernommen werden?
	 */
	private static void startApp(String path, boolean usePathAsRootDir) {
		System.out.println("***INITIALIZING CLASSIFICATION DIPLOMAS***");
		List<Diploma> diplomas = new ArrayList<>();
		File f = new File(path);
		if(!f.exists()){
			System.err.println("The path you entered is invalid!");
			return;
		}
		if(f.isDirectory()){
			diplomas = initializeDiplomas(path, pp);
		} else{
			String fileName = f.getName();
			if(!correctFileType(fileName)){
				return;
			}
			Diploma dipl = new Diploma(f.getName(), f, pp);
			diplomas.add(dipl);
		}
		if(!checkSuccess(new ArrayList<AbstractDiploma>(diplomas))) return;
		System.out.println("Found "+diplomas.size()+" diplomas for classification.");
		System.out.println();

		System.out.println("***STARTING CLASSIFICATION***");
		System.out.println();
		classify(new ArrayList<AbstractDiploma>(diplomas), dc, pc);
		System.out.println();

		System.out.println("DIPLOMA_ANALYZER_APP: Generating new labeled XML files for classified diplomas...");
		System.out.println();
		String rootPath = null;
		if(usePathAsRootDir){
			rootPath = path;
		}
		for(Diploma dipl : diplomas){
			dipl.generateLabeledXML(newPath, rootPath);
		}
		System.out.println();
		System.out.println("DIPLOMA_ANALYZER_APP: Finished classifying for path '"+path+"'");
		System.out.println("\tThe output XML files are stored in: "+newPath+" (relative to the src directory of the program code)");
	}

	/**
	 * Klassifiziert alle Sätze der übergebenen Urkunden mithilfe des DiplomaticClassifiers und ProbabilisticClassifiers.
	 * @param diplomas Die zu klassifizierenden Urkunden.
	 * @param dc Der DiplomaticClassifier, der genutzt werden soll.
	 * @param pc Der ProbabilisticClassifier, der genutzt werden soll.
	 */
	static void classify(List<AbstractDiploma> diplomas, DiplomaticClassifier dc, ProbabilisticClassifier pc) {
		
		List<AbstractSentence> allSentences = new ArrayList<>();
		for(AbstractDiploma dipl : diplomas){
			allSentences.addAll(dipl.getSentences());
		}
		
		// pro Satz regelbasierte Wahrscheinlichkeiten für jedes Label berechnen 
		System.out.println("DIPLOMATIC_CLASSIFIER: Assigning sequence based label probabilities to classification sentences...");
		dc.assignSequenceBasedPropabilities(allSentences);
		
		// Berechnung der Label-Wahrscheinlichkeiten der Sätze mithilfe von probabilistischer Klassifizierung
		System.out.println("PROBABILISTIC_CLASSIFIER: Assigning probabilistic label probabilities to classification sentences...");
		pc.classify(allSentences);
		
		// indikatorbasierte Label-Zuweisung
		System.out.println("DIPLOMATIC_CLASSIFIER: Assigning indicator based labels to classification sentences...");
		dc.assignByIndicators(allSentences);
		
		// getrennte Sätze gleichen Labels verbinden  
		System.out.println("DIPLOMATIC_CLASSIFIER: Assigning labels based on sequence filling to classification sentences...");
		dc.fillSequence(diplomas);
		
		// übrige Sätze wiederum regelbasiert zuweisen
		System.out.println("DIPLOMATIC_CLASSIFIER: Assigning labels based on overall label probability to classification sentences...");
		dc.assignByDefault(diplomas);

		System.out.println("DIPLOMATIC_CLASSIFIER: Assigning paragraph labels to classification sentences...");
		dc.assignParagraphsByPartLabels(allSentences);
	}
	
	/**
	 * Liest nacheinander alle Dateien aus, die sich im durch den path
	 * spezifizierten Ordner befinden und erstellt für jede Datei ein TrainingDiploma-Objekt.
	 * @param Der Pfad, aus dem die cei.xml Dateien ausgelesen werden sollen.
	 * @return Eine Liste mit den ausgelesenen Trainingsdiplomata.
	 */
	static List<TrainingDiploma> initializeTrainingDiplomas(String path, Preprocessor pp) {	

		File origDataDir = new File(path);

		List<TrainingDiploma> diplomas = new ArrayList<TrainingDiploma>();
		if(!checkFilesDir(origDataDir, path)){
			return diplomas;
		}
		
		diplomas = getTrainingDiplomas(origDataDir, pp);
		
		if(!checkSuccess(new ArrayList<AbstractDiploma>(diplomas))){
			System.out.println("The program is terminated due to lack of training data.");
			System.exit(0);
		}
		
		return diplomas;
	}
	
	private static List<TrainingDiploma> getTrainingDiplomas(File startFile, Preprocessor pp) {
		List<TrainingDiploma> diplomas = new ArrayList<>();
		File[] files = startFile.listFiles();
		for(File file : files){
			String fileName = file.getName();
			if(file.isDirectory()){
				diplomas.addAll(getTrainingDiplomas(file, pp));
				continue;
			}
			if(!correctFileType(fileName)) continue;
			
			TrainingDiploma dipl = new TrainingDiploma(fileName, file, pp);
			if(dipl.useDiploma()){
				diplomas.add(dipl);
			} else {
				System.out.println("The file "+fileName+" is not included in further calculations.");
			}
			
		}
		return diplomas;
	}

	/**
	 * Liest nacheinander alle Dateien aus, die sich im durch den path
	 * spezifizierten Ordner befinden und erstellt für jede Datei ein Diploma-Objekt.
	 * @param Der Pfad, aus dem die cei.xml Dateien ausgelesen werden sollen.
	 * @return Eine Liste mit den ausgelesenen Diplomata.
	 */
	private static List<Diploma> initializeDiplomas(String path, Preprocessor pp) {	

		File origDataDir = new File(path);
		List<Diploma> diplomas = new ArrayList<>();
		if(!checkFilesDir(origDataDir, path)){
			return diplomas;
		}
		
		diplomas = getDiplomas(origDataDir, pp);
		
		return diplomas;
	}
	
	private static List<Diploma> getDiplomas(File startFile, Preprocessor pp) {
		List<Diploma> diplomas = new ArrayList<>();
		File[] files = startFile.listFiles();
		for(File file : files){
			String fileName = file.getName();
			if(file.isDirectory()){
				diplomas.addAll(getDiplomas(file, pp));
				continue;
			}
			if(!correctFileType(fileName)) continue;
			
			Diploma dipl = new Diploma(fileName, file, pp);
			if(dipl.useDiploma()){
				diplomas.add(dipl);
			} else {
				System.out.println("The file "+fileName+" is not included in further calculations.");
			}
			
		}
		return diplomas;
	}

	private static boolean checkFilesDir(File file, String path){
		if(!file.isDirectory()){
			System.err.println("The given path "+path+" doesn't lead to a directory. The path must define a directory containing the xml/cei documents that shall be used in the classification training.");
			System.out.println("Exit program.");
			return false;
		}
		return true;
	}
	
	private static boolean correctFileType(String fileName){
		if(fileName.endsWith(".cei.xml")){
			return true;
		} else {
			System.err.println("The file "+fileName+" has the wrong document type. The type needs to be .xml or .cei. The file is not read.");
			return false;
		}
	}
	
	private static boolean checkSuccess(List<AbstractDiploma> diplomas){
		if(diplomas.isEmpty()){
			System.err.println("No diplomas found. Either you did give a directory which has no files, or one that has only files with a wrong document type.");
			return false;
		}
		return true;
	}
}
