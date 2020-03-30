package dataClasses.evaluationResults;

import java.util.ArrayList;
import java.util.List;

import dataClasses.label.DiplomaticLabel;
/**
 * Datenklasse zur Verwaltung der Micro-Evaluationswerte über alle LabelEvaluationResults.
 * @author Alina Ostrowski
 *
 */
public class MicroAverageResult {

	private double precision = 0;
	private double recall = 0;
	private double accuracy = 0;
	private double f1Value = 0;
	
	/**
	 * Liste all jener Label, die bei der Berechnung nicht berücksichtigt wurden
	 */
	List<DiplomaticLabel> unused;
	
	/**
	 * Berechnet die Evaluationswerte Recall, Precision, Accuracy und F1-Maß als Durchschnitt übder die Einzelwerte.
	 * @param labelResults Die Resultate, die die Basis der Durchschnittsberechnung bilden.
	 */
	public MicroAverageResult(List<LabelEvaluationResult> labelResults) {

		double tpSum = 0;
		double tnSum = 0;
		double fpSum = 0;
		double fnSum = 0;
		
		unused = new ArrayList<>();		
		for(LabelEvaluationResult result : labelResults){

			// wenn das Label nicht berücksichtigt werden soll, da es in den Trainingsdaten nicht vorkommt, füge es zu unused hinzu
			// und bezieh es nicht in die Berechnungen ein
			if(!result.isUsed()){
				unused.add(result.getLabel());
				continue;
			}
			tpSum += result.getTp();
			tnSum += result.getTn();
			fpSum += result.getFp();
			fnSum += result.getFn();
		}
		
		precision = tpSum / (tpSum + fpSum);
		recall = tpSum / (tpSum + fnSum);
		accuracy = (tpSum + tnSum) / (tpSum + fpSum + tnSum + fnSum);
		f1Value = 2 * ((precision * recall) / (precision + recall));

	}

	public List<DiplomaticLabel> getUnused() {
		return unused;
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

}
