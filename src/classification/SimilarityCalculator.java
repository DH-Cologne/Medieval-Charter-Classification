package classification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Der SimilarityCalculator bietet Funktionen zur Berechnung von String-�hnlichkeiten an.
 * @author Alina Ostrowski
 *
 */
public class SimilarityCalculator {
	
	/**
	 * Pr�ft, ob ein Substring in einem anderen String vorhanden ist. Die Methode wirkt
	 * �hnlich wie die String-Methode contains() mit folgenden Unterschieden: <br>
	 * <ul><li>Der Vergleich arbeitet nicht mit Chars, sondern mit W�rtern (Tokens, die anhand von Whitespaces erkannt werden)</li>
	 * <li>Der Vergleich arbeitet fehlertolerant, d. h. zwei Tokens werden als identisch erkannt, wenn sie gem�� der Needleman-Wunsch-Similarity als �hnlich gelten. </li></ul>
	 * @param compare Der gesuchte Substring.
	 * @param orig Der String, in dem der Substring gesucht werden soll.
	 * @return true, wenn der Substring im Originalstring enthalten ist; sonst false.
	 */
	public boolean containsSimilarTokenSubstring(String compare, String orig) {

		List<String> compareTokens = Arrays.asList(compare.split("\\s+"));
		List<String> origTokens = Arrays.asList(orig.split("\\s+"));
		
		List<Integer> possibleStartIndexes = new ArrayList<>();
		
		// M�gliche Start-Indizes des Substrings suchen, Bsp.: F�r Substring "Ich gehe" sind m�gliche Anf�nge in "Ich esse und ich gehe." die Indizes 0 und 3.
		for(int i = 0; i < origTokens.size(); i++){
			String currentOrigToken = origTokens.get(i); 
			if(isNWsimilar(currentOrigToken, compareTokens.get(0))){
				possibleStartIndexes.add(i);
			}
		}
		
		// f�r jeden m�glichen Startindex versuchen, den Substring zu finden
		startLoop: for(Integer start : possibleStartIndexes){
			int nextIndex = start+1;
			
			compareLoop: for(int j = 1;j < compareTokens.size(); j++){
				String currentCompareToken = compareTokens.get(j);
				// Suche das erste Wort des zu vergleichenden Strings und gehe dann zum n�chsten Wort.
				for(int i = nextIndex; i < origTokens.size(); i++){
					String origToken = origTokens.get(i);
					if(isNWsimilar(currentCompareToken, origToken)){
						// falls das Wort gefunden wird beginne die Suche nach dem n�chsten Wort ab dem n�chsth�heren Index
						nextIndex = i+1;
						continue compareLoop;
					} else{
						continue startLoop;
					}
				}
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Gibt an, ob der Originalstring *alle* Tokens des Vergleichsstrings enth�lt. Die einzelnen Tokens werden dabei per Needleman-Wunsch-�hnlichkeit
	 * verglichen, d.h. der Vergleich arbeitet fehlertolerant gegen�ber orthographischen Abweichungen.
	 * @param compare Der String, dessen Tokens enthalten sein m�ssen
	 * @param orig Der String, in dem die compare-Tokens enthalten sein m�ssen
	 * @return true, wenn alle Tokens des compare-Strings im orig-String enthalten sind, sonst false.
	 */
	public boolean containsAllTokens(String compare, String orig) {

		List<String> compareTokens = Arrays.asList(compare.split("\\s+"));
		List<String> origTokens = Arrays.asList(orig.split("\\s+"));
	
		for(String currentCompareToken : compareTokens){
			boolean foundToken = false;
			for(String origToken: origTokens){
				if(isNWsimilar(currentCompareToken, origToken)){
					foundToken = true;
					break;
				}
			}
			if(!foundToken){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * <p>Gibt die �hnlichkeit des zu vergleichenden Substrings zum entsprechenden Substring des Originalstrings zur�ck (basierend auf dem Jaccard-Koeffizienten). Die
	 * Methode wirkt �hnlich wie containsSimilarTokenSubstring(), mit dem Unterschied, dass eine �hnlichkeit auch dann erkannt wird, wenn zwischen den einzelnen
	 * Tokens des Substrings auch Tokens auftauchen, die nicht zum compare-Substring geh�ren, ebenso wenn einzelne Tokens gar nicht oder ein einer anderen Reihenfolge
	 * auftauchen.</p>
	 * Damit ein Substring erkannt wird, m�ssen <b>mindestens 75 % aller Tokens</b> des compare-Strings sich auch im Originalstring finden, und zwar unabh�ngig von deren Reihenfolge.</p><p>Der Abgleich der Tokens
	 * arbeitet dabei fehlertolerant gegen�ber orthographischen Abweichungen einzelner Tokens, d. h. zwei Tokens werden als identisch erkannt, sobald sie
	 * gem�� Needleman-Wunsch-Wahrscheinlichkeit �hnlich sind.</p><p>Kann ein entsprechender Substring im Originalstring gefunden werden, so wird 0 zur�ckgegeben.</p>
	 * @param compare Der zu vergleichende Substring.
	 * @param orig Der String, in dem der Substring gefunden werden soll.
	 * @return Den Jaccard-Koeffizienten des compare-Strings zum entsprechenden Substring des Originalstrings (0, falls kein entsprechender Substring gefunden wurde).
	 */
	public double similarityOfSubstring(String compare, String orig){
		
		// 1. Substring und alle W�rter dazwischen finden
		List<String> compareTokens = Arrays.asList(compare.split("\\s+"));
		List<String> origTokens = Arrays.asList(orig.split("\\s+"));
		List<String> normalizedOrigTokens= findSubset(compareTokens, origTokens);
		
		if(normalizedOrigTokens == null){
			return 0;
		}
		
		// 2. Stringsimilarity zwischen dem compare-String und dem entsprechenden Substring des orig-Strings berechnen und zur�ckgeben
		double similarity = jaccardCoefficient(new HashSet<String>(compareTokens), new HashSet<String>(normalizedOrigTokens));
		
		return similarity;
	}

	
	private List<String> findSubset(List<String> compareTokens, List<String> origTokens) {
		// finde Substring unabh�ngig von der Reihenfolge
		List<String> normalizedOrigTokens = new ArrayList<>();
		int lowestSubStringIndex = origTokens.size();
		int highestSubStringIndex = 0;
		
		// teste, welche der Vergleichstokens im OriginalString enthalten sind (unabh�ngig von der Reihenfolge)
		origLoop: for(String origToken: origTokens){ 
			for(String currentCompareToken : compareTokens){
				if(isNWsimilar(currentCompareToken, origToken)){
					normalizedOrigTokens.add(currentCompareToken);
					int origTokenIndex = origTokens.indexOf(origToken);
					if(lowestSubStringIndex > origTokenIndex){
						lowestSubStringIndex = origTokenIndex;
					}
					if(highestSubStringIndex < origTokenIndex){
						highestSubStringIndex = origTokenIndex;
					}
					continue origLoop;
				}
			}
			normalizedOrigTokens.add(origToken);
		}

		int notFound = 0;
		for(String currentCompareToken : compareTokens){
			if(!normalizedOrigTokens.contains(currentCompareToken))
				notFound++;
		}
		
		// Wenn zu viele der gesuchten W�rter des Vergleichssubstrings nicht gefunden wurden, gib null zur�ck
		if((notFound/compareTokens.size())>0.25){
			return null;
		}
		
		List<String> sublist = normalizedOrigTokens.subList(lowestSubStringIndex, highestSubStringIndex+1);
		
		// gib die normalisierte TokenListe zur�ck, allerdings nur den Teil, der vom ersten gefundenen bis zum letzten gefundenen Vergleichstoken reicht
		return sublist;
	}
	
	/**
	 * Berechnet den Jaccard-Koeffizienten zweier Strings. Dieser Abgleich arbeitet auf Tokenbasis, nicht auf Characterbasis.
	 * @param tokenSet1
	 * @param tokenSet2
	 * @return Ein Wert zwischen 0 und 1. Je h�her der Wert, desto �hnlicher sind sich zwei Strings.
	 */
	private double jaccardCoefficient(Set<String> tokenSet1, Set<String> tokenSet2) {
	
		Set<String> joinedTokens = new HashSet<>();
		joinedTokens.addAll(tokenSet1);
		joinedTokens.addAll(tokenSet2);
		
		tokenSet1.retainAll(tokenSet2);		
		
		return (double) tokenSet1.size() / joinedTokens.size();
	}
	
	/**
	 * Gibt an, ob zwei Strings sich �hneln. Zum Abgleich wird der Needleman-Wunsch-Algorithmus genutzt,
	 * so dass der Abgleich tolerant gegen�ber kleineren Abweichungen ist.
	 * @param s1
	 * @param s2
	 * @return true, wenn die �bergebenen Strings sich �hneln. Sonst false.
	 */
	private boolean isNWsimilar(String s1, String s2) {
	
		int nwSimilarity = needlemanWunschSimilarity(s1.trim(), s2.trim());
		
		if(nwSimilarity >= (s1.trim().length() + s2.trim().length()))
			return true;
			return false;
	}
		
	
	/**Diese Methode sowie die von ihr genutzten Methoden compare(char a, char b) und max(int a, int b, int c) sind �bernommen aus dem GitHub-Repository des Forschungsprojektes
	 * "Qualifikationsentwicklungsforschung" des Instituts f�r Digital Humanities der Universit�t zu K�ln (siehe Link unten, zuletzt abgerufen
	 * am 28.07.2019). Die Methoden sind unver�ndert mit der Ausnahme, dass der im dortigen Projekt als Feldvariable existierende <br>
	 * <code>int gap = -1</code> <br>
	 * direkt in die Methode eingef�gt wurde.<br>
	 * Die Methode berechnet die Needleman-Wunsch-�hnlichkeit der zwei �bergebenen Strings.
	 * @param s1
	 * @param s2
	 * @return needleman-wunsch-similarity
	 * {@link} https://github.com/spinfo/quenfo/blob/master/src/main/java/quenfo/de/uni_koeln/spinfo/categorization/workflow/SimilarityCalculator.java
	 */
	private int needlemanWunschSimilarity(String s1, String s2){
		
			// eingef�gte Methodenvariable [AO]:
			int gap = -1;
			
			char[] a = s1.toCharArray();
			char[] b = s2.toCharArray();

			int n = a.length;
			int m = b.length;

			int[][] d = new int[n + 1][m + 1];

			d[0][0] = 0;
			for (int i = 0; i <= n; i++) {
				d[i][0] = -i;
			}
			for (int j = 0; j <= m; j++) {
				d[0][j] = -j;
			}

			for (int i = 1; i <= n; i++) {
				for (int j = 1; j <= m; j++) {
					d[i][j] = max(d[i - 1][j - 1] + compare(a[i - 1], b[j - 1]), d[i][j - 1] + gap, d[i - 1][j] + gap);
				}
			}
			return d[n][m];
	}
	private int max(int a, int b, int c) {
		int max = 0;
		if (a > max)
			max = a;
		if (b > max)
			max = b;
		if (c > max)
			max = c;
		return max;
	}
	private int compare(char a, char b) {
		int match = 3;
		int mismatch = 0;
		if (a == b)
			return match;
		return mismatch;
	}
	
}
