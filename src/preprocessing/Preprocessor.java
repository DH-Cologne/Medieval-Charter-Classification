package preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import dataClasses.sentence.AbstractSentence;
import dataClasses.sentence.TrainingSentence;
import helpers.ReaderWriter;

/**
 * Der Preprocessor stellt Methoden zur Text-Normalisierung sowie zur Lemmatisierung und Vektorisierung bereit.<br>
 * Information: Zur Lemmatisierung wird das externe Programm "LEMLAT 3.0" genutzt. Die hier verwendete Instanz ist
 * ausgelegt für Windows und kann auf anderen Betriebssystemen möglicherweise Fehler verursachen.
 * @author Alina Ostrowski
 *
 */
public class Preprocessor {
	
	// Listen mit Regex-Paaren zur Normalisierung eines lateinischen Textes
	private static List<String[]> capitalLetterResolvers = new ArrayList<>();
	private List<String[]> paranthesisAnnotations;
	private List<String[]> resolverPairs;
	private List<String[]> abbreviationPairs;

	/**
	 * Feldvariable zur Speicherung aller bisher lemmatisierten Tokens und der dazugehörigen Lemmata
	 */
	Map<String, String> lemmaPairs;
	
	public Preprocessor(String resolverPath, String abbreviationPath, String capitalLetterPath, String paranthesisPath) {

		resolverPairs = ReaderWriter.readCSV(new File(resolverPath), true, ",", 2);
		abbreviationPairs = ReaderWriter.readCSV(new File(abbreviationPath), true, ",", 3);
		capitalLetterResolvers = ReaderWriter.readCSV(new File(capitalLetterPath), true, ",", 2);
		paranthesisAnnotations = ReaderWriter.readCSV(new File(paranthesisPath), true, ",", 2);
		lemmaPairs = new HashMap<>();
	}

	/**
	 * Lemmatisiert die bisher unlemmatisierten Tokens der übergebenen Satz-Objekte und speichert die lemmatisierten Tokens auf
	 * der lemmatizedTokens-Feldvariable des entsprechenden Satzobjektes. Zur Lemmatiesierung wird der Lemmatizer "LEMLAT 3.0" benutzt
	 * (siehe JavaDoc zur privaten Methode getPossibleLemmasForTypes()).
	 * @param sentences Satz-Objekte, deren Tokens lemmatisiert werden sollen.
	 */
	public void lemmaTokenizeSentences(List<AbstractSentence> sentences) {

		// Set mit allen Types erstellen
		Set<String> unlemmatizedTypes = new HashSet<>();
		for(AbstractSentence sent : sentences){
			unlemmatizedTypes.addAll(sent.getTokens());
		}
		
		// Alle Wortformen, die bereits lemmatisiert wurden, sollen nicht noch einmal lemmatisiert werden, darum entfernen
		unlemmatizedTypes.removeAll(lemmaPairs.keySet());

		if(unlemmatizedTypes.size() > 0){
			System.out.println("... fetching possible lemmata for unknown wordForms...");
			// eine Map mit allen möglichen Lemmata (Value) pro unlemmatisiertem Token (Key) erstellen lassen
			Map<String, List<String>> allLemmaMap = getPossibleLemmasForTypes(unlemmatizedTypes);
			
			// Aus der Liste an möglichen Lemmata pro unlemmatisiertem Token das Lemma heraussuchen, welches am häufigsten vorkommt
			// und es der Lemma-Paar-Map hinzufügen
			System.out.println("... finding best lemma per token...");
			updateLemmaPairs(allLemmaMap);
		} else {
			System.out.println("... all wordforms known.");
		}
		
		// Den Sätzen die entsprechenden Lemmata zuweisen:
		// Für jedes unlemmatisierte Token des Satzes das entsprechende Lemma aus der lemmaPairs-Liste heraussuchen und dem
		// Satz alle Lemmata zuweisen
		System.out.println("... assigning lemmata to sentences...");
		for(AbstractSentence sent : sentences){
			List<String> unlemmatizedTokens = sent.getTokens();
			List<String> lemmatizedTokens = new ArrayList<>();
			for(String unlemTok : unlemmatizedTokens){
				lemmatizedTokens.add(lemmaPairs.get(unlemTok));
			}
			sent.setLemmatizedTokens(lemmatizedTokens);
		}

		System.out.println("... finished Lemmatizing.");
	}
	
	/**
	 * Sucht für alle unlemmatisierten Types (Keyset der übergebenen Map) dasjenige Lemma aller möglichen Lemmata (Value) heraus,
	 * welches am häufigsten auftaucht. Wenn ein oder mehrere Lemmata gleich häufig auftauchen, wird per default das erste dieser
	 * Lemmata ausgewählt. Der Type und sein so gefundenes "bestes" Lemma werden der lemmaPairs-Map des Preprocessor-Objekts hinzugefügt.  
	 * @param lemmaMap Eine Map mit einem unlemmatisierten Type als Key und den dazugehörigen möglichen Lemmata in einer Liste als Value.
	 */
	private void updateLemmaPairs(Map<String, List<String>> lemmaMap) {
		for(Entry<String, List<String>> allLemmas : lemmaMap.entrySet()){
			String token = allLemmas.getKey();
			List<String> possibleLemmas = allLemmas.getValue();
			
			// Wenn nur ein mögliches Lemma gefunden wurde, dieses direkt zur lemmaPairs-Map hinzufügen
			if(new HashSet<String>(possibleLemmas).size() == 1){
				lemmaPairs.put(token, possibleLemmas.get(0));
				continue;
			}
			
			// Wenn mehrere mögliche Lemmata gefunden wurden, die Häufigkeit aller Lemmata zählen und das häufigste
			// der lemmaPairs-Map hinzufügen
			Map<String, Integer> tokenLemmasCount = new HashMap<>();
			String bestLemma = "";
			int lemmaCount = 0;
			for(String lemma : possibleLemmas){
				
				if(tokenLemmasCount.containsKey(lemma)){
					tokenLemmasCount.put(lemma, tokenLemmasCount.get(lemma)+1);
				} else {
					tokenLemmasCount.put(lemma, 1);
				}
				int newCount = tokenLemmasCount.get(lemma);
				if(newCount > lemmaCount){
					lemmaCount = newCount;
					bestLemma = lemma;
				}
			}
			
			lemmaPairs.put(token, bestLemma);
		}
	}

	/**
	 * Erstellt mithilfe des Lemmatizers "LEMLAT 3.0" mögliche Lemmata für die übergebenen Tokens und gibt diese zurück. 
	 * Tritt während der Abfrage des externen Lemmatizers ein Problem auf oder kann für eine Wortform kein Lemma gefunden werden,
	 * so setzt die Methode für diese Wortform das unlemmatisierte Wort selbst als Lemma ein.<br>
	 * Der hier benutzte Lemmatizer ist "LEMLAT 3.0".
	 * Homepage: {@link http://www.lemlat3.eu/}. <br>Die genaue Instanz ist die Windows Embedded Version: <br>
	 * {@link https://github.com/CIRCSE/LEMLAT3/blob/master/bin/windows_embedded.zip}
	 * @param allTypes Die zu lemmatisierenden Wortformen.
	 * @return Eine Map mit einer Liste aller möglichen Lemmata (Value) zu jedem übergebenen Token (Key).
	 */
	private Map<String, List<String>> getPossibleLemmasForTypes(Set<String> allTypes){
		Map<String, List<String>> allLemmaMap = new HashMap<>();
		
		//Strings, die im Output von Lemlat.exe das Auftauchen einer (unlemmatisierten) Wortform bzw. eines Lemmas markieren
		String wordformDeclarator = "Input    wordform :";
		String lemmaDeclarator = "	============================LEMMA ";
		
		// Da die Lemmatisierung relativ lange dauert, soll sie in Etappen von 500 Wortformen geschehen.
		// Darum Aufsplittung des Type-Sets in subSets von 500 Wörtern.
		List<String> allTypesList = new ArrayList<>(allTypes);
		List<Set<String>> subSets = new ArrayList<>();
	
		while(allTypesList.size()>500){
			Set<String> typesSubList = new HashSet<String>(allTypesList.subList(0, 500));
			allTypesList.removeAll(typesSubList);
			subSets.add(typesSubList);
		}
		subSets.add(new HashSet<>(allTypesList));
		
		// Zählvariablen für die Konsolenausgabe
		int count = 0;
		int size = allTypes.size();
		
		for(Set<String> types : subSets){
			Process lemlat = null;
			try {
				// den Prozess lemlat.exe starten. Der Ordner, in dem das Programm laufen soll,
				// muss explizit definiert werden, da das Programm sonst aus dem src-Ordner heraus zu arbeiten
				// versucht und dann nicht auf die Lemlat-Datenbanken zugreifen kann
				lemlat = Runtime.getRuntime().exec("lemlat/lemlat.exe", null, new File("lemlat"));
				
				try(BufferedReader br = new BufferedReader(new InputStreamReader(lemlat.getInputStream()));
				PrintWriter pw = new PrintWriter(lemlat.getOutputStream());){

					// Alle zu lemmatisierenden types in den OutputStream schreiben, der zur lemlat.exe führt 
					for(String type : types){
						count++;
						pw.println(type);
					}
					System.out.println("... requested lemmata for the first "+count+" of "+size+" wordforms.");
					pw.close();

					// Aus dem InputStream, der von der lemlat.exe geschickt wird, alle Wortformen sowie möglichen Lemmata herausfiltern
					// Zur Erkennung, wo im Output sich eine Wortform oder ein Lemma befindet, werden die oben definierten Strings
					// wordformDeclarator und lemmaDeclarator genutzt
					String currentWordform = null;
					List<String> currentLemmas = new ArrayList<>();
					boolean lineContainsLemma = false;

					String nextLine = br.readLine();
					while(nextLine != null){
						if(lineContainsLemma){
							String[] lineParts = nextLine.trim().split("\\s+");
							currentLemmas.add(lineParts[0]);
							lineContainsLemma = false;
						} else if(nextLine.startsWith(wordformDeclarator)){
							if(currentWordform != null){
								// Wenn keine Lemmata gefunden wurden, dann wird das unlemmatisierte Token selbst als Lemma hinzugefügt
								if(currentLemmas.isEmpty()){
									currentLemmas.add(currentWordform);
								}
								allLemmaMap.put(currentWordform, currentLemmas);
								currentLemmas = new ArrayList<>();
							}
							currentWordform = nextLine.replace(wordformDeclarator, "").trim().toLowerCase();
						} else if(nextLine.startsWith(lemmaDeclarator)){
							lineContainsLemma = true;
						}
						nextLine = br.readLine();
					}
					if(currentWordform != null){
						if(currentLemmas.isEmpty()){
							currentLemmas.add(currentWordform);
						}
						allLemmaMap.put(currentWordform, currentLemmas);
					}
					br.close();
					System.out.println("...fetched possible lemmata for "+count+ " of "+size+" wordforms.");
				}
	            
				// Den Lemlat-Prozess beenden
	            lemlat.destroy();
			} catch (Exception e) {
				System.out.println("Problem occurred while communicating with LEMLAT 3.0. Lemmatization results could be bad.");
				if(lemlat != null && lemlat.isAlive()){
					lemlat.destroy();
				}
				e.printStackTrace();
			}
			//allen bisher unlemmatisierten Wortformen sich selbst als Lemma zuweisen (kann bei Problemen mit dem Lemmatizer passieren)
			for(String type : types){
				if(!allLemmaMap.containsKey(type)){
					List<String> ownLemma = new ArrayList<String>();
					ownLemma.add(type);
					allLemmaMap.put(type, ownLemma);
				}
			}
		}

		return allLemmaMap;
	}
	
	/**
	 * Erzeugt die Bigramme eines Satzes auf Basis der bereits lemmatisierten Tokens.
	 * Besteht ein Satz nur aus einem Token, dann wird das einzelne Token als einzelnes "Bigramm"
	 * gewertet. Die Bigramme werden auf der bigrams-Feldvariable des Sentence-Objekts gespeichert.
	 * @param sentences Die Sätze, für die Bigramme erzeugt werden sollen.
	 */
	public void lemmatizedTokensToBigrams(List<AbstractSentence> sentences){
		for(AbstractSentence sentence : sentences){
			List<String> sentLemmas = sentence.getLemmatizedTokens();
			if(sentLemmas.size() > 1){
				List<String> bigrams = new ArrayList<>();
				for(int i = 0; i < sentLemmas.size()-1; i++){
						bigrams.add(sentLemmas.get(i)+" "+sentLemmas.get(i+1));
				}
				sentence.setBigrams(bigrams);
			} else{ // wenn der Satz nur aus einem Wort besteht
				sentence.setBigrams(sentLemmas);
			}
		}
	}
	
	/**
	 * Vektorisiert eine Liste von Sätzen gemäß des übergebenen Vektor-Typs. Die erzeugten Vektoren werden auf der vector-Feldvariable
	 * des Sentence-Objekts gespeichert.
	 * @param sentences Die zu vektorisierenden Sätze.
	 * @param useBigramsInsteadOfTokens Soll mit Bigrammen statt mit Tokens gearbeitet werden?
	 * @param vectorType Welche Berechnungsart soll für die Vektorisierung genutzt werden?
	 * @param totalTypeFrequencies Eine Map, die als Keyset alle Types des Trainingskorpus enthält. Der Wert entspricht jeweils der Anzahl an Dokumenten, in denen der Type vorkommt.
	 * @param trainingSentenceCount Die Gesemtzahl der Trainingssätze (Trainingsdokumente).
	 * @param typeVector Eine Liste mit allen Types des Trainingskorpus. Die Vektor-Werte werden anhand der Anordnung dieser Liste erzeugt.
	 */
	public void vectorize(List<AbstractSentence> sentences, boolean useBigramsInsteadOfTokens, VectorType vectorType,
			Map<String, Integer> totalTypeFrequencies, int trainingSentenceCount, List<String> typeVector) {
		
		for(AbstractSentence sentence : sentences){
	
			switch(vectorType){
				case tfIdf:
					tfidfVectorize(sentence, useBigramsInsteadOfTokens, typeVector, totalTypeFrequencies, trainingSentenceCount);
					break;
				case count:
					countVectorize(sentence, useBigramsInsteadOfTokens, typeVector);
					break;
				case binary:
					binaryVectorize(sentence, useBigramsInsteadOfTokens, typeVector);
					break;
				}
		}
	}
	
	/**
	 * Erzeugt einen binären Vektor und speichert ihn auf der vector-Feldvariable des Sentence-Objektes.<br>
	 * D.h.: Unabhängig von der absoluten Häufigkeit des Types, ist sein Wert 1, wenn er im Satz vorkommt, und 0, wenn er nicht vorkommt.
	 * @param sentence Der zu vektorisierende Satz.
	 * @param useBigramsInsteadOfTokens Soll mit Bigrammen statt mit Tokens gearbeitet werden?
	 * @param typeVector Eine Liste mit allen Types des Trainingskorpus. Die Vektor-Werte werden anhand der Anordnung dieser Liste erzeugt.
	 */
	private void binaryVectorize(AbstractSentence sentence, boolean useBigramsInsteadOfTokens, List<String> typeVector) {
		
		double[] vector = zeroVector(typeVector.size());

		List<String> sentTokens;
		if(useBigramsInsteadOfTokens) sentTokens = sentence.getBigrams();
		else sentTokens = sentence.getLemmatizedTokens();
		
		Set<String> sentTypes = new HashSet<>(sentTokens);
		
		for(int i = 0; i < typeVector.size(); i++){
			if(sentTypes.contains(typeVector.get(i))){
				vector[i] = 1.0;
			}
		}
		
		sentence.setVector(vector);
				
	}

	/**
	 * Erzeugt einen Häufigkeitsvektor und speichert ihn auf der vector-Feldvariable des Sentence-Objektes.<br>
	 * D.h.: Der Wert eines Types entspricht der absoluten Häufigkeit des Types im Satz.
	 * @param sentence Der zu vektorisierende Satz.
	 * @param useBigramsInsteadOfTokens Soll mit Bigrammen statt mit Tokens gearbeitet werden?
	 * @param typeVector Eine Liste mit allen Types des Trainingskorpus. Die Vektor-Werte werden anhand der Anordnung dieser Liste erzeugt.
	 */
	private void countVectorize(AbstractSentence sentence, boolean useBigramsInsteadOfTokens, List<String> typeVector) {
	
		double[] vector = zeroVector(typeVector.size());
		
		List<String> sentTokens;
		if(useBigramsInsteadOfTokens) sentTokens = sentence.getBigrams();
		else sentTokens = sentence.getLemmatizedTokens();
		
		for(String token : sentTokens){
			int index = typeVector.indexOf(token);
			if(index >= 0)	vector[index] = vector[index] + 1.0;
		}
		
		sentence.setVector(vector);
			
	}

	/**
	 * Erzeugt einen tf-idf-Vektor und speichert ihn auf der vector-Feldvariable des Sentence-Objektes.<br>
	 * D.h.: Der Wert eines Types entspricht dem tfIdf-Wert des Types innerhalb des Trainingskorpus in Relation zum Satz.
	 * @param sentence Der zu vektorisierende Satz.
	 * @param useBigramsInsteadOfTokens Soll mit Bigrammen statt mit Tokens gearbeitet werden?
	 * @param typeVector Eine Liste mit allen Types des Trainingskorpus. Die Vektor-Werte werden anhand der Anordnung dieser Liste erzeugt.
	 */
	private void tfidfVectorize(AbstractSentence sentence, boolean useBigramsInsteadOfTokens, List<String> typeVector, Map<String, Integer> totalTypeFrequencies, int trainingSentenceCount) {
		
		countVectorize(sentence, useBigramsInsteadOfTokens, typeVector);
		double[] vector = zeroVector(typeVector.size());

		double[] absFreq = sentence.getVector();

		double highestFreq = 0;
		for(int i = 0; i < absFreq.length; i++){
			double freq = absFreq[i];
			if(freq > highestFreq){
				highestFreq = freq;
			}
		}

		int i = 0;
		for (String type : typeVector) {
			double totalTypeFreq = totalTypeFrequencies.get(type);
			
			if(highestFreq == 0 || totalTypeFreq == 0){
				continue;
			}
			
			double tf = absFreq[i] / highestFreq;
			double idf = (double) trainingSentenceCount / totalTypeFreq;
			Double tfIdf = tf * Math.log(idf);
			vector[i] = tfIdf;
			i++;
		}
		
		sentence.setVector(vector);
		
	}

	private double[] zeroVector(int size){
		double[] vector = new double[size];
		for(int i = 0; i < vector.length; i++){
			vector[i] = 0;
		}
		return vector;
	}
	
	/**
	 * Entfernt editorische Anmerkungen aus dem Text des Satzes oder löst sie, wo möglich, zu Wörtern auf. Löst lateinische Abkürzungen auf.
	 * Normalisiert den so entstandenen Text mit der Methode normalizeText(String text). 
	 * @param sent Der Satz, dessen Text normalisiert werden soll.
	 */
	private void clearSentenceText(AbstractSentence sent){
		
			String text = sent.getText().trim();
			text = replaceByRegexPair(text, paranthesisAnnotations);
			text = replaceByRegexPair(text, resolverPairs);
			text = replaceByRegexPair(text, abbreviationPairs);
			sent.setText(normalizeLatinText(text));
		
	}
	
	/**
	 * Normalisiert den übergebenen Text, indem folgende Modifikationen durchgeführt werden:<br>
	 * <ul>
	 * <li>überflüssige Whitespaces entfernen</li>
	 * <li>Diakritika entfernen</li>
	 * <li>Alle Nicht-Buchstaben- oder Ziffer-Zeichen entfernen</li>
	 * <li>Lateinische Schreibweise anpassen: v - u; j - i; ae am Wortende - e</li>
	 * <li>Text toLowerCase()</li>
	 * <li>Römische Zahlen durch die Platzhalterzahl 123 ersetzen</li>
	 * <li>Großgeschriebene Wörter innerhalb des Strings (nicht am String-Anfang) durch das Platzhalterwort "namedEntity" ersetzen</li>
	 * </ul>
	 * @param text Der zu normalisierende String
	 * @return Der normalisierte String
	 */
	public static String normalizeLatinText(String text){
		
		text = text.trim();
		
		// mehrere whitespaces durch einfache whitespaces ersetzen, u.a., um den text von Zeilenumbrüchen zu reinigen
		text = text.replaceAll("\\s+", " ");	

		// Ersetze alle römischen Zahlen sowie deren Abkürzungspunkte durch die Platzhalterziffer 123
		text = text.replaceAll("(?<=[^\\w]|^)[MmDdIiCcVvXx°]+\\.*(?=[^\\w]|$)" , " 123 ");

		// Diakritika entfernen
		text = Normalizer.normalize(text, Normalizer.Form.NFD);
		text = text.replaceAll("[^\\p{ASCII}]", "");
		
		// Wörter, die meist großgeschrieben werden, allerdings nicht als Eigennamen erkannt werden sondern bestehen bleiben sollen, klein schreiben
		text = replaceByRegexPair(text, capitalLetterResolvers);
		
		// Ersetze alle großgeschriebenen Wörter innerhalb eines Satzes (nicht am Satzbeginn) durch den Platzhalternamen "namedEntity"
		text = text.replaceAll("(?<!\\.|(\\.[\\w\\s]))[A-Z]\\w+", " namedEntity ");
		
		text = text.toLowerCase();
		
		// Ersetze alles mit leeren Strings, was nicht einer der folgenden character ist: a-z 0-9 whitespace
		text = text.replaceAll("[^a-z0-9\\s]", "");
		
		// Lateinische Schreibweise vereinheitlichen
		text = text.replaceAll("v", "u");
		text = text.replaceAll("j", "i");
		text = text.replaceAll("(?<=\\w)ae(?!\\w)", "e"); //ae am Ende eines Wortes wird zu e -> latinae -> latine, aber aeternitas -> aeternitas
		
		// Durch die Ersetzungen evtl. entstandene doppelte Whitespaces wieder kürzen
		text = text.replaceAll("\\s+", " ");
		
		return text;
	}
	
	private static String replaceByRegexPair(String text, List<String[]> regexPairs) {
		
		for(String[] regexPair : regexPairs){
			String regex = regexPair[0];
			String replacement = regexPair[1];
			try{
				text = text.replaceAll(regex, replacement);
			} catch(Exception e){
				System.out.println("Problem applying the regex pair '"+regex+", "+replacement+"'. The pair isn't used. Exception is:");
				e.printStackTrace();
			}
		}
		
		return text;
	}

	/**
	 * Convenience-Methode: Führt für alle übergebenen Sätze die Methode prepareSentence() aus.
	 * @param sentences Die Sentence-Objekte, die vorbereitet werden sollen.
	 */
	public void prepareAll(List<AbstractSentence> sentences) {
		for(AbstractSentence sent : sentences){
			prepareSentence(sent);
		}
	}
	
	/**
	 * Bereitet den übergebenen Satz auf die weiterführende Verarbeitung vor, d.h. normalisiert den Text des übergebenen Satz-Objektes mithilfe der Methode clearSentenceText() und erstellt auf Basis
	 * des normalisierten Textes Satztokens, die auf der tokens-Feldvariable des Satzobjektes gespeichert werden.
	 * @param sent Der Satz, der vorbereitet werden soll.
	 */
	public void prepareSentence(AbstractSentence sent){

		// alle Sätze von editorischen Anmerkungen und Unklarheiten reinigen sowie Abkürzungen auflösen
		clearSentenceText(sent);

		// Tokenisierung der Sätze		
		List<String> tokens = new ArrayList<String>(Arrays.asList(sent.getText().trim().split("\\s+")));
		// leere Strings und Whitespaces entfernen
		List<String> falseTokens = new ArrayList<>();
		for(String token : tokens){
			if(token.length() == 0 || token.matches("\\s+")) falseTokens.add(token);
		}
		tokens.removeAll(falseTokens);
		sent.setTokens(tokens);
	}

	/**
	 * Berechnet auf Basis aller Types der übergebenen Trainingssätze die absolute Dokumentenhäufigkeit eines jeden Types, d.h. in wievielen
	 * Dokumenten ein bestimmter Type verkommt.
	 * @param trainingSentences Die Trainingssätze, die als Berechnungsbasis dienen sollen.
	 * @param useBigramsInsteadOfTokens Soll mit Bigrammen statt mit Tokens gearbeitet werden?
	 * @return Eine Map mit allen Types als Keyset und jeweils der Dokumentenhäufigkeit eines jeden Types als Value.
	 */
	public Map<String, Integer> createTypeFrequenciesMap(List<TrainingSentence> trainingSentences, boolean useBigramsInsteadOfTokens) {
		Map<String, Integer> totalTypeFrequencies = new HashMap<>();

		for(AbstractSentence sent : trainingSentences){
			List<String> sentTokens;
			if(useBigramsInsteadOfTokens) sentTokens = sent.getBigrams();
			else sentTokens = sent.getLemmatizedTokens();
			Set<String> sentTypes = new HashSet<String>(sentTokens);
			
			for (String type : sentTypes) {
				int freq = 0;
				if(totalTypeFrequencies.containsKey(type)){
					freq = totalTypeFrequencies.get(type);
				}
				totalTypeFrequencies.put(type, freq + 1);
			}	
		}
		return totalTypeFrequencies;
	}

	public List<String[]> getParanthesisAnnotations() {
		return paranthesisAnnotations;
	}

	public void setParanthesisAnnotations(List<String[]> paranthesisAnnotations) {
		this.paranthesisAnnotations = paranthesisAnnotations;
	}

	public List<String[]> getAbbreviationPairs() {
		return abbreviationPairs;
	}

	public void setAbbreviationPairs(List<String[]> abbreviationPairs) {
		this.abbreviationPairs = abbreviationPairs;
	}
}
