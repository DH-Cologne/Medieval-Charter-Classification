package dataClasses.evaluationResults;

import dataClasses.label.DiplomaticLabel;

/**
 * Datenklasse zur Verwaltung der Evaluationswerte pro Label.
 * @author Alina Ostrowski
 *
 */
public class LabelEvaluationResult {

	private DiplomaticLabel label;
	
	private int tp;
	private int fp;
	private int tn;
	private int fn;

	private double precision;
	private double recall;
	private double accuracy;
	private double f1Value;
	
	private boolean used;
	
	/**
	 * Initialisiert die Feldvariablen der Klasse und berechnet die Evaluationswerte Recall, Precision, Accuracy und F1-Maß
	 * @param label Das Label, für das die Resultate gelten.
	 * @param tp true positives
	 * @param fp false positives
	 * @param tn true negatives
	 * @param fn false negatives
	 */
	public LabelEvaluationResult(DiplomaticLabel label, int tp, int fp, int tn, int fn) {

		this.label = label;
		 
		this.tp = tp;
		this.fp = fp;
		this.tn = tn;
		this.fn = fn;

		// Evaluationmaße Precision, Recall, Accuracy und F1-Maß berechnen
		
		if(tp == 0 && fn == 0){
			
			// Wenn es weder true positives noch false negatives gibt, dann schließt sich daraus, dass das Label in den Trainingsdaten niemals
			// tatsächlich vorkam und die Evaluationsmaße über die Güte des Klassifikators in Bezug auf dieses Label nichts aussagen können,
			// darum soll das Label nicht in die Berechnung mit einfließen und used wird auf false gesetzt
			used = false;
			
			recall = 0;
		} else {
			used = true;
			recall =  ((double)tp / (double)(tp + fn));
		}
		;
		if(tp == 0 && fp == 0){
			precision = 0;
		} else{
			precision = ((double)tp / (double)(tp + fp));
		}
		
		accuracy = ((double)(tp + tn) / (double)(tp + fp + tn + fn));
		if(precision == 0 && recall == 0){
			f1Value = 0;
		} else {
			f1Value = 2 * ((precision * recall) / (precision + recall));
		}
	}

	public double getPrecision() {
		return precision;
	}

	public double getRecall() {
		return recall;
	}

	public double getAccuracy() {
		return accuracy;
	}

	public double getF1Value() {
		return f1Value;
	}
	
	public int getTp() {
		return tp;
	}

	public int getFp() {
		return fp;
	}

	public int getTn() {
		return tn;
	}

	public int getFn() {
		return fn;
	}

	public boolean isUsed() {
		return used;
	}

	public DiplomaticLabel getLabel() {
		return label;
	}

}
