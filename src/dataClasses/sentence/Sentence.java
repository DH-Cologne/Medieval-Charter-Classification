package dataClasses.sentence;

import dataClasses.diploma.AbstractDiploma;

/**
 * Datenklasse für ein einfaches Satz-Objekt (ohne bekanntes Label).
 * @author Alina Ostrowski
 *
 */
public class Sentence extends AbstractSentence {

	public Sentence(AbstractDiploma diploma, String text) {
		super(diploma, text);
	}
	
}
