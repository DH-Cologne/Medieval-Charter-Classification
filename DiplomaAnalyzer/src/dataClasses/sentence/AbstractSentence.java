package dataClasses.sentence;

import java.util.List;

import dataClasses.diploma.AbstractDiploma;
import dataClasses.label.DiplomaticLabel;
import dataClasses.label.DiplomaticParagraphLabel;

/**
 * Superdatenklasse für alle Satzobjekte. Sie verwaltet die basalen Eigenschaften des Satzes
 * und bietet Methoden zur Aktualisierung der Label-Wahrscheinlichkeiten des Satzes.
 * @author Alina Ostrowski
 *
 */
public abstract class AbstractSentence {

	/**
	 * Index des Satzes innerhalb der Urkunde.
	 */
	protected int index;
	/**
	 * Index zwischen 0 und 1 relativ zur Gesamt(wort)länge der Urkunde.
	 * Je niedriger der relativeIndex, desto weiter vorne steht der Satz.
	 */
	protected double wordRelativeIndex;
	
	// Index des ersten bzw. letzten Wortes des Satzes
	private int indexOfFirstWord;
	private int indexOfLastWord;
	
	// Invertierter Index des ersten bzw. letzten Wortes des Satzes, d.h. relativ zum Urkunden-Ende statt -Anfang
	private int inversedIndexOfFirstWord;
	private int inversedIndexOfLastWord;
	
	/**
	 * Text des Satzes
	 */
	protected String text;
	/**
	 * Urkunde, zu der der Satz gehört
	 */
	protected AbstractDiploma diploma;
	
	// Feldvariablen, auf die später die zugewiesenen Label gelegt werden sollen
	protected DiplomaticParagraphLabel paragraphLabel;
	protected DiplomaticLabel partLabel;

	// Feldvariablen, auf den später die Tokens / Lemmata / Bigramme des Satzes gelegt werden sollen
	protected List<String> tokens;
	protected List<String> lemmatizedTokens;
	protected List<String> bigrams;
	
	/**
	 * Vektor des Satzes, der durch den ProbabilisticClassifier und den Preprocessor erzeugt wird
	 */
	protected double[] vector;
	
	/**
	 * Wahrscheinlichkeit eines jeden Labels für diesen Satz
	 */
	private double[] labelProbabilities;

	public AbstractSentence(AbstractDiploma diploma, String text){
		this.diploma = diploma;
		this.text = text;
		initializeLabelProbabilities();
	}

	/**
	 * Initialisiert die Label-Wahrscheinlichkeiten des Satz-Objektes mit dem Wert 1.0 an allen Indexes
	 */
	protected void initializeLabelProbabilities() {
		int length = DiplomaticLabel.values().length;
		this.labelProbabilities = new double[length];
		for(int i = 0; i < length; i++){
			labelProbabilities[i] = 1.0;
		}
	}

	/**
	 * Verrechnet die übergebenen Label-Wahrscheinlichkeiten mit den Label-Wahrscheinlichkeiten des Satzobjektes,
	 * indem beide Wahrscheinlichkeiten miteinander multipliziert werden.
	 * @param newImpacts Die Label-Wahrscheinlichkeiten, mit denen die bisherigen verrechnet werden sollen.
	 */
	public void updateLabelProbability(double[] newImpacts) {
		for(int i = 0; i < labelProbabilities.length; i++){
			labelProbabilities[i] = labelProbabilities[i] * newImpacts[i];
		}
	}

	/**
	 * Setzt die Wahrscheinlichkeit eines bestimmten Labels auf den übergebenen Wert.
	 * @param index Index desjenigen Labels, für das die neue Wahrscheinlichkeit gilt.
	 * @param newImpact Die neue Wahrscheinlichkeit für das Label.
	 */
	public void setLabelProbability(int index, double newImpact) {
		labelProbabilities[index] = newImpact;
	}
	
	public double[] getLabelProbabilities() {
		return labelProbabilities;
	}

	public void setLabelProbabilities(double[] labelProbabilities) {
		this.labelProbabilities = labelProbabilities;
	}
	
	/**
	 * Gibt an, ob dem Satz bereits ein Part-Label zugewiesen wurde.
	 * @return true, falls der Satz ein Part-Label hat, sonst false.
	 */
	public boolean hasLabel(){
		if(this.partLabel != null) return true;
		else return false;
	}
	
	public int getIndex() {
		return index;
	}

	public double getWordRelativeIndex() {
		return wordRelativeIndex;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public double[] getVector() {
		return vector;
	}

	public void setVector(double[] vector) {
		this.vector = vector;
	}

	public DiplomaticLabel getLabel() {
		return partLabel;
	}

	public void setPartLabel(DiplomaticLabel label) {
		this.partLabel = label;
	}
	
	public DiplomaticParagraphLabel getParagraphLabel() {
		return paragraphLabel;
	}

	public void setParagraphLabel(DiplomaticParagraphLabel paragraphLabel) {
		this.paragraphLabel = paragraphLabel;
	}

	public AbstractDiploma getDiploma() {
		return diploma;
	}

	public void setLemmatizedTokens(List<String> lemmatizedTokens) {
		this.lemmatizedTokens = lemmatizedTokens;
	}
	
	public List<String> getLemmatizedTokens() {
		return lemmatizedTokens;
	}

	public void setBigrams(List<String> bigrams) {
		this.bigrams = bigrams;
	}
	
	public List<String> getBigrams(){
		return bigrams;
	}
	
	public void setWordRelativeIndex(double d){
		this.wordRelativeIndex = d;
	}
	
	public void setIndex(int i){
		this.index = i;
	}

	public List<String> getTokens() {
		return tokens;
	}

	public void setTokens(List<String> tokens) {
		this.tokens = tokens;
	}

	public int getIndexOfFirstWord() {
		return indexOfFirstWord;
	}

	public void setIndexOfFirstWord(int indexOfFirstWord) {
		this.indexOfFirstWord = indexOfFirstWord;
	}

	public int getIndexOfLastWord() {
		return indexOfLastWord;
	}

	public void setIndexOfLastWord(int indexOfLastWord) {
		this.indexOfLastWord = indexOfLastWord;
	}
	
	public int getInversedIndexOfFirstWord() {
		return inversedIndexOfFirstWord;
	}

	public void setInversedIndexOfFirstWord(int inversedIndexOfFirstWord) {
		this.inversedIndexOfFirstWord = inversedIndexOfFirstWord;
	}

	public int getInversedIndexOfLastWord() {
		return inversedIndexOfLastWord;
	}

	public void setInversedIndexOfLastWord(int inversedIndexOfLastWord) {
		this.inversedIndexOfLastWord = inversedIndexOfLastWord;
	}

}
