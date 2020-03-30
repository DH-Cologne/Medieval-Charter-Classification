package config;

import preprocessing.VectorType;

/**
 * Konfigurations-Klasse, die für die Einstellungen der Klassifikation genutzt wird.
 * @author Alina Ostrowski
 *
 */
public class ClassificationConfig {
	private int sequProbsTolerance;
	private VectorType vectorType;
	private boolean useBigramsInsteadOfTokens;
	
	/**
	 * Erstellt eine neue ClassificationConfig mit folgenden Default-Werten:<br>
	 * <ul>
	 * <li> Toleranzwert für die Berechnung der Labelwahrscheinlichkeit auf Basis der Satzposition = 1 </li>
	 * <li> Vektorisierungsmethode für die probabilistische Klassifikation = binär </li>
	 * <li> Soll bei der Tokenisierung nach Bigrammen statt Tokens unterteilt werden? = Ja </li> 
	 * </ul>
	 */
	public ClassificationConfig(){
		sequProbsTolerance = 1;
		vectorType = VectorType.binary;
		useBigramsInsteadOfTokens = true;
	}
	
	/**
	 * Erstellt eine neue ClassificationConfig mit den übergebenen Werten.
	 * @param sequProbsTolerance Toleranzwert für die Berechnung der Labelwahrscheinlichkeit auf Basis der Satzposition.
	 * @param vectorType Vektorisierungsmethode für die probabilistische Klassifikation.
	 * @param useBigramsInsteadOfTokens Soll bei der Tokenisierung nach Bigrammen (true) oder Tokens (false) unterteilt werden?
	 */
	public ClassificationConfig(int sequProbsTolerance,	VectorType vectorType, boolean useNGramsInsteadOfTokens){
		this.sequProbsTolerance = sequProbsTolerance;
		this.vectorType = vectorType;
		this.useBigramsInsteadOfTokens = useNGramsInsteadOfTokens;
	}

	public int getSequProbsTolerance() {
		return sequProbsTolerance;
	}

	public VectorType getVectorType() {
		return vectorType;
	}

	public boolean getUseBigramsInsteadOfTokens() {
		return useBigramsInsteadOfTokens;
	}
}
