package dataClasses.evaluationResults;

import java.util.ArrayList;
import java.util.List;

import dataClasses.label.DiplomaticLabel;
/**
 * Datenklasse zur Verwaltung der Macro-Evaluationswerte über alle LabelEvaluationResults.
 * @author Alina Ostrowski
 *
 */
public class MacroAverageResult {

	private double precision = 0;
	private double recall = 0;
	private double accuracy = 0;
	private double f1Value = 0;
	
	/**
	 * Liste all jener Label, die bei der Berechnung nicht berücksichtigt wurden
	 */
	List<DiplomaticLabel> unused;
	
	/**
	 * Berechnet die Evaluationswerte Recall, Precision, Accuracy und F1-Maß als Gesamtdurchschnitt.
	 * @param labelResults Die Resultate, die die Basis der Durchschnittsberechnung bilden.
	 */
	public MacroAverageResult(List<LabelEvaluationResult> labelResults) {

		unused = new ArrayList<>();
		for(LabelEvaluationResult result : labelResults){
			
			// wenn das Label nicht berücksichtigt werden soll, da es in den Trainingsdaten nicht vorkommt, füge es zu unused hinzu
			// und bezieh es nicht in die Berechnungen ein
			if(!result.isUsed()){
				unused.add(result.getLabel());
				continue;
			}
			precision += result.getPrecision();
			recall += result.getRecall();
			accuracy += result.getAccuracy();
			f1Value += result.getF1Value(); 
		}
		
		int labelsCount = labelResults.size() - unused.size();
		
		precision /= labelsCount;
		recall /= labelsCount;
		accuracy /= labelsCount;
		f1Value /= labelsCount;
		
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
