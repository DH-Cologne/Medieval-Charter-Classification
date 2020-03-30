package dataClasses.sentence;

import dataClasses.diploma.AbstractDiploma;
import dataClasses.label.DiplomaticLabel;
import dataClasses.label.DiplomaticParagraphLabel;

/**
 * Datenklasse für einen TrainingSentence. Jeder TrainingSentence muss ab seiner Initialisierung
 * sowohl ein bekanntes partLabel als auch ein bekanntes paragraphLabel besitzen.
 * @author Alina Ostrowski
 *
 */
public class TrainingSentence extends AbstractSentence {

	// die bekannten Label des Satzes
	private DiplomaticParagraphLabel trueParagraphLabel;
	private DiplomaticLabel truePartLabel;
	
	/**
	 * Text des Satzes vor jeglicher Normalisierung
	 */
	private String rawText;

	public TrainingSentence(AbstractDiploma diploma, String text, DiplomaticParagraphLabel trueParagraphLabel, DiplomaticLabel truePartLabel) {
		super(diploma, text);
		this.trueParagraphLabel = trueParagraphLabel;
		this.truePartLabel = truePartLabel;
		this.rawText = text;
	}

	/**
	 * Setzt die Felder des Satz-Objekts auf ihren Ursprungszustand bei Initialisierung zurück, d.h.:
	 * <ul>
	 * <li>Paragraph- sowie Partlabel sind null.</li>
	 * <li>Der Text des Satzes ist derjenige vor der Präprozessierung.</li>
	 * <li>Jeder Wert der Label-Probabilities ist gleich 1.</li>
	 * <li>Tokens, Bigramme sowie Satzvektor existieren nicht und sind darum null.</li>
	 * </ul>
	 */
	public void reset() {
		this.paragraphLabel = null;
		this.partLabel = null;
		this.text = rawText;
		this.initializeLabelProbabilities();
		this.lemmatizedTokens = null;
		this.tokens = null;
		this.bigrams = null;
		this.vector = null;
	}

	public DiplomaticParagraphLabel getTrueParagraphLabel() {
		return trueParagraphLabel;
	}
	
	public DiplomaticLabel getTruePartLabel() {
		return truePartLabel;
	}
}
