# Medieval-Charter-Classification
Program that allows to detect and classify the segments of medieval royal charters according to their diplomatic formulae.

# Erkennung und Klassifizierung der Formularbestandteile mittelalterlicher Königsurkunden #
Dieses Repository enthält den Code für ein Programm, welches mittelalterliche Königsurkunden, die in Form von CEI-XML-Dateien vorliegen, in die Abschnitte des Urkundenformulars zerlegen, diese klassifizieren und annotieren kann.

Eine dataillierte **technische Dokumentation sowie Evaluation** des Programms befindet sich unter documentation/Technische_Dokumentation.md. Am Ende dieser Readme sind unter "Änderungen" alle Eingriffe aufgelistet, die nach Anfertigung dieser Dokumentation vorgenommen worden sind. Unter documentation/evaluationResults_Kopie befinden sich außerdem .txt-Dateien, die **die genauen Evaluationswerte** (Recall, Precision, Accuracy, F1-Score, Micro- und Macro-Averages) für verschiedene Programmkonfigurationen enthalten. Die Evaluationsdateien werden bereitgestellt für den Fall, dass es aus technischen Gründen nicht möglich sein sollte, den Programmablauf in voller Funktion nachzubilden, siehe dazu unten "Benutzungshinweis".

Die **Trainingsdaten** für das dazu benutzte Machine Learning müssen CEI-annotierte Urkunden mit Volltext sein, in denen auch die Formularbestandteile mit den entsprechenden Tags (\<protocol\>, \<invocatio\>, \<dispositio\> etc.) ausgezeichnet sind. Die Dateien müssen im Ordner data/testData liegen und können dort in weitere Unterordner aufgeteilt sein befinden.

Die bei der Erstellung des Programms benutzten Trainingsdaten stammen aus dem virtuellen Urkundenarchiv [Monasterium.net](https://www.monasterium.net/mom/home). Sie wurden eigenständig um die Annotation der Formularbestandteile ergänzt. Die Dateien befinden sich in einem eigenen Repository: <https://github.com/AlinaOs/Structurally_Annotated_Medieval_Charters>. Die hier angegebenen Evaluationsergebnisse beziehen sich auf die Nutzung dieser Trainingsdaten.

Der **Naive Bayes Classifier**, der für das Machine Learning genutzt wurde, wurde über die pom.xml in das Programm eingebunden. Es handelt sich um die [Weka-Library der Waikato-Universität Neuseeland](https://www.cs.waikato.ac.nz/ml/weka/index.html).

Eine **Lemmatisierung** wurde durch die Stand-Alone-Version des Lemmatizers [LEMLAT 3.0](http://www.lemlat3.eu/) eingebunden.

Für **String-Abgleiche** wurde u.a. eine Funktion zur Berechnung der Needleman-Wunsch-Ähnlichkeit genutzt, die aus dem GitHub-Repository des [Forschungsprojektes "Qualifikationsentwicklungsforschung"](https://github.com/spinfo/quenfo/blob/master/src/main/java/quenfo/de/uni_koeln/spinfo/categorization/workflow/SimilarityCalculator.java) des Instituts für Digital Humanities der Universität zu Köln übernommen wurde.

## Benutzungshinweis ##
Das Programm arbeitet mit einem externen **Lemmatizer**, der nicht als Library eingebunden ist. Um Urheberrechte nicht zu verletzen, konnte die Stand-Alone-Version von LEMLAT 3.0 hier nicht hochgeladen werden. Wenn Sie den Programm-Code herunterladen, um ihn selbst auszuprobieren oder zu nutzen, wird das Programm also zunächst ohne Lemmatisierung arbeiten, d.h. es wird funktionieren, jedoch möglicherweise schlechte Ergebnisse liefern. Um die volle Funktion des Programms testen zu können, müssen Sie darum zunächst selbstständig LEMLAT 3.0 einbetten:

Die Stand-Alone-Version für Windows, Linux, OSX kann im GitHub-Repository von LEMLAT 3.0 heruntergeladen werden: <https://github.com/CIRCSE/LEMLAT3/blob/master/bin/windows_embedded.zip>
Das Klassifikationsprogramm wurde auf Windows entwickelt, so dass es möglich ist, dass auf anderen Betriebssystemen Programmfehler auftreten, obwohl die LEMLAT-Version für diese Betriebssysteme geeignet ist.
Die heruntergeladene und entpackte Anwendung muss mit allen dazugehörigen Datenbanken und Ordnerstrukturen (data, share, lemlat.dtd, lemlat.exe, my.cnf) im Ordner lemlat abgelegt werden. Danach müsste das Programm auf die Anwendung zugreifen können, so dass die Lemmatisierung Ergebnisse liefert.

Für das **Training** des Naive Bayes-Algorithmus sowie für eine Evaluation des Programms ist eine ausreichende Menge von **Trainingsdaten** vonnöten. Diese müssen im Ordner data/testData abgelegt werden. Um das Programm auf dem eigenen Rechner zu testen, können beispielsweise die Dateien im externen Repository <https://github.com/AlinaOs/Structurally_Annotated_Medieval_Charters/> in den Ordner kopiert werden. Das Programm kann dann darauf zugreifen und das Klassifikationsmodell damit trainieren.

Selbstverständlich benötigt das Programm als **Eingabedaten** ebenfalls Urkundentexte. Diese müssen ebenfalls im CEI-Format vorliegen, jedoch ohne die Tags für das Urkundenformular im Volltext zu enthalten. Diese zu klassifizierenden Dateien können im Ordner data/inputData abgelegt werden. Im Programm kann dann der Standardmodus gewählt weren, um diesen Ordner automatisch als Quelle für die Klassifikations-Urlunden zu nutzen. Alternativ kann im Programm auch ein Pfad zu einem anderen Ordner oder einzelnen Urkunden angegeben werden, die sich auf dem eigenen Rechner befinden. Die Evaluation hingegen lässt sich auch ohne Klassifikationsurkunden und nur mit Trainingsurkunden durchführen.

## Änderungen ##
Veränderungen am Code oder an den Trainingsdokumenten im Gegensatz zum in der technischen Dokumentation beschriebenen Zustand:

19.03.2020: Anpassung der Trainingsbasis von 89 Urkunden; 6 Urkunden wurden nachträglich entfernt. Gründe:
- 1 ausgestellt von Friedrich II., dessen Urkunden nicht in das Korpus mit aufgenommen werden sollten
- 2 liegen nicht mehr im gewählten Zeitraum von 932-1214
- 1 Urkunde Ottos II. von 972, die wortwörtlich eine Urkunde seines Vaters kopiert, der diese am selben Tag ausstellte
- 2 Fälschungen bzw. vermutete Fälschungen
-> Daraus folgt, dass das Korpus nun 83 Urkunden enthält.

21.04.2020: Überarbeitung der Trainingsbasis
- Überarbeitung der Trainingsdokumente im Hinblick auf die Grenze zwischen *narratio* und *dispositio*, da die vorherigen Grenzen teils nicht korrekt gesetzt waren.
- Update der Kopien der Evaluationsergebnisse: Da eine fehlerhafte Trainingsbasis die Evaluationswerte des Programms sowohl zum Positiven als auch zum Negativen verfälschen kann, wurde das Programm erneut auf das verbesserte Korpus angewendet und die im Zuge dessen ausgegebenen Evaluationsdateien in das Repository überführt.
- Im Zuge dessen wurde der Evaluationsklasse eine Funktion hinzugefügt, die neben den Ergebnisdateien für einzelne Konfigurationen auch eine Datei mit der Gesamtübersicht über die Ergebnisse aller Konfigurationen erstellt.
