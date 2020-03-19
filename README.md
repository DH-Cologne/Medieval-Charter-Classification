# Medieval-Charter-Classification
Program that allows to detect and classify the segments of medieval royal charters according to their diplomatic formulae.

# Erkennung und Klassifizierung der Formularbestandteile mittelalterlicher Königsurkunden #
Dieses Repository enthält den Code für ein Programm, welches mittelalterliche Königsurkunden, die in Form von CEI-XML-Dateien vorliegen, in die Abschnitte des Urkundenformulars zerlegen, diese klassifizieren und annotieren kann.

Eine dataillierte **technische Dokumentation sowie Evaluation** des Programms befindet sich unter documentation/Technische_Dokumentation.pdf. Ganz am Ende dieser Readme sind unter "Änderungen" alle Eingriffe aufgelistet, die nach Anfertigung dieser Dokumentation vorgenommen worden sind. Unter documentation/evaluationResults_Kopie befinden sich außerdem .txt-Dateien, die **die genauen Evaluationswerte** (Recall, Precision, Accuracy, F1-Score, Micro- und Macro-Averages) für verschiedene Programmkonfigurationen enthalten. Die Evaluationsdateien werden bereitgestellt für den Fall, dass es aus technischen Gründen nicht möglich sein sollte, den Programmablauf in voller Funktion nachzubilden, siehe dazu unten "Benutzungshinweis".

Die **Trainingsdaten** für das dazu benutzte Machine Learning stammen aus dem virtuellen Urkundenarchiv [Monasterium.net](https://www.monasterium.net/mom/home). Sie wurden eigenständig um die Annotation der Formularbestandteile (\<protocol\>, \<invocatio\>, \<dispositio\> etc.) ergänzt. Die Dateien befinden sich im Ordner data/testData. Jede Datei enthält die originalen Meta-Daten der Urkunden (Stand ca. Herbst 2018), so dass diese identifizierbar sind. Weiter unten in dieser Readme befindet sich außerdem eine Konkordanz mit Hinweisen zu den jeweiligen Archiven und Sammlungen, aus denen die Urkunden stammen.

Der **Naive Bayes Classifier**, der für das Machine Learning genutzt wurde, wurde über die pom.xml in das Programm eingebunden. Es handelt sich um die [Weka-Library der Waikato-Universität Neuseeland](https://www.cs.waikato.ac.nz/ml/weka/index.html).

Eine **Lemmatisierung** wurde durch die Stand-Alone-Version des Lemmatizers [LEMLAT 3.0](http://www.lemlat3.eu/) eingebunden.

Für **String-Abgleiche** wurde u.a. eine Funktion zur Berechnung der Needleman-Wunsch-Ähnlichkeit genutzt, die aus dem GitHub-Repository des [Forschungsprojektes "Qualifikationsentwicklungsforschung"](https://github.com/spinfo/quenfo/blob/master/src/main/java/quenfo/de/uni_koeln/spinfo/categorization/workflow/SimilarityCalculator.java) des Instituts für Digital Humanities der Universität zu Köln übernommen wurde.

## Benutzungshinweis ##
Das Programm arbeitet mit einem externen Lemmatizer, der nicht als Library eingebunden ist. Um Urheberrechte nicht zu verletzen, konnte die Stand-Alone-Version von LEMLAT 3.0 hier nicht hochgeladen werden. Wenn Sie den Programm-Code herunterladen, um ihn selbst auszuprobieren oder zu nutzen, wird das Programm also zunächst ohne Lemmatisierung arbeiten, d.h. es wird funktionieren, jedoch möglicherweise schlechte Ergebnisse liefern. Um die volle Funktion des Programms testen zu können, müssen Sie darum zunächst selbstständig LEMLAT 3.0 einbetten:

Die Stand-Alone-Version für Windows, Linux, OSX kann im GitHub-Repository von LEMLAT 3.0 heruntergeladen werden: <https://github.com/CIRCSE/LEMLAT3/blob/master/bin/windows_embedded.zip>
Das Klassifikationsprogramm wurde auf Windows entwickelt, so dass es möglich ist, dass auf anderen Betriebssystemen Programmfehler auftreten, obwohl die LEMLAT-Version für diese Betriebssysteme geeignet ist.
Die heruntergeladene und entpackte Anwendung muss mit allen dazugehörigen Datenbanken und Ordnerstrukturen (data, share, lemlat.dtd, lemlat.exe, my.cnf) im Ordner lemlat abgelegt werden.

Danach müsste das Programm auf die Anwendung zugreifen können, so dass die Lemmatisierung Ergebnisse liefert.

## Konkordanz ##
Es folgt eine Auflistung der auf Monasterium.net vertretenen Archive und Sammlungen, aus denen Urkunden zum Training des Machine Learning-Algorithmus benutzt worden sind.
Die Trainingsbasis beinhaltet Urkunden aus folgenden Archiven:

### Österreich, Haus- Hof- und Staatsarchiv (AT-HHStA) ###
Bestände: Salzburg, Domkapitel (831-1802) (SbgDK); Salzburg, Erzstift (798-1806) (SbgE)
Monasterium: <https://www.monasterium.net/mom/AT-HHStA/archive>
Website: <https://www.oesta.gv.at/haus-hof-und-staatsarchiv1>
- SbgDK/AUR_0953_XI_29.cei.xml
- SbgDK/AUR_1207_XII_10.cei.xml
- SbgDK/AUR_1062_VIII_23.cei.xml
- SbgDK/AUR_1059_VI_01.cei.xml
- SbgDK/AUR_1056_VII_04.cei.xml
- SbgDK/AUR_1056_VII_03.cei.xml
- SbgDK/AUR_1055_III_22.cei.xml
- SbgDK/AUR_1049_II_06-12.cei.xml
- SbgDK/AUR_1045_XII_07.cei.xml
- SbgDK/AUR_1027_VII_26.cei.xml
- SbgDK/AUR_1020_IV_23.cei.xml
- SbgDK/AUR_1014_VI_21.cei.xml
- SbgDK/AUR_1006_XII_07.cei.xml
- SbgDK/AUR_0985_X_17.cei.xml
- SbgDK/AUR_0970_III_07.cei.xml
- SbgE/AUR_1209_II_20.cei.xml
- SbgE/AUR_1207_IX_22.cei.xml
- SbgE/AUR_1199_IX_29.cei.xml
- SbgE/AUR_1072_II_04.cei.xml
- SbgE/AUR_1057_II_04.cei.xml
- SbgE/AUR_1055_III_06.cei.xml
- SbgE/AUR_1051_II_08.cei.xml
- SbgE/AUR_0996_V_28.cei.xml
- SbgE/AUR_0982_V_18.cei.xml
- SbgE/AUR_0977_X_01.cei.xml


### Österreich, Oberösterreichisches Landesarchiv (AT-OOeLA) ###
Bestände: Urkunden Garsten (1082-1778) (GarstenOSB); Mondsee, Benediktiner (1104-1802) (MondseeOSB)
Monasterium: <https://www.monasterium.net/mom/AT-OOeLA/archive>
Website: <https://www.landesarchiv-ooe.at/>
- GarstenOSB/1142.ce.xml
- MondseeOSB/1104_II_27.cei.xml

### Österreich, Göttweig, Stiftsarchiv (AT-StiAG) ###
Bestände: Urkunden (1058-1899) (GoettweigOSB)
Monasterium: <https://www.monasterium.net/mom/AT-StiAG/archive>
Website: <https://www.stiftgoettweig.at/>
- GoettweigOSB/1058_X_26.cei.xml
- GoettweigOSB/1066_VII_17.cei.xml
- GoettweigOSB/1108_IX_06.cei.xml

### Österreich, Kremsmünster, Stiftsarchiv (AT-StiAKr) ###
Bestände: Urkunden (777-1894) (KremsmuensterOSB)
Monasterium: <https://www.monasterium.net/mom/AT-StiAKr/archive>
Website: <https://stift-kremsmuenster.net/>
- KremsmuensterOSB/0975_VI_11.cei.xml
- KremsmuensterOSB/0975_VI_21.cei.xml
- KremsmuensterOSB/1052_VII_20.cei.xml
- KremsmuensterOSB/1063_X_25.cei.xml

### Österreich, Lambach, Stiftsarchiv (AT-StiAL) ###
Bestände: Urkunden (992-1600) (LambachOSB)
Monasterium: <https://www.monasterium.net/mom/AT-StiAL/archive>
Website: <http://www.stift-lambach.at/>
- LambachOSB/1061_II_18.1.cei.xml
- LambachOSB/1162_II_26.cei.xml

### Österreich, Reichersberg, Stiftsarchiv (AT-StiAR) ###
Bestände: Urkunden (1137-1857) (ReichersbergCanReg)
Monasterium: <https://www.monasterium.net/mom/AT-StiAR/archive>
Website: <https://www.stift-reichersberg.at/>
- ReichersbergCanReg/1162_IV_04.ce.xml

### Österreich, St. Florian, Stiftsarchiv (AT-StiASF) ###
Bestände: Urkunden (900-1797) (StFlorianCanReg)
Monasterium: <https://www.monasterium.net/mom/AT-StiASF/archive>
Website: <http://www.stift-st-florian.at/start.html>
- StFlorianCanReg/1109_XI_04.cei.xml
- StFlorianCanReg/1125_XI_20.cei.xml
- StFlorianCanReg/1142.1.cei.xml
- StFlorianCanReg/1212_V_21.cei.xml

### Schweiz, St. Gallen, Stiftsarchiv (CH-StiASG) ###
Bestände: St. Gallen, Stiftsarchiv (1004-1500) (Urkunden); Urkunden Pfäfers (861-1500) (StiAPfae)
Monasterium: <https://www.monasterium.net/mom/CH-StiASG/archive>
Website: <https://www.sg.ch/kultur/stiftsarchiv.html>
- Urkunden/A.1.A.13.cei.xml
- Urkunden/A.1.A_14.cei.xml
- Urkunden/FF.3.S.1.cei.xml
- StiAPfae/0000.10.cei.xml
- StiAPfae/0000.32.cei.xml
- StiAPfae/0000.294.cei.xml
- StiAPfae/0000.363.cei.xml
- StiAPfae/0000.399.cei.xml
- StiAPfae/0000.406.cei.xml
- StiAPfae/0000.470.cei.xml
- StiAPfae/0000.472.cei.xml

### Deutschland, München, Bayerisches Hauptstaatsarchiv (DE-BayHStA) ###
Bestände: Kloster Raitenhaslach Urkunden (Zisterzienser 1034-1798) (KURaitenhaslach)
Monasterium: <https://www.monasterium.net/mom/DE-BayHStA/archive>
Website: <https://www.gda.bayern.de/archive/hauptstaatsarchiv/>
- KURaitenhaslach/1034_05_08.cei.xml
- KURaitenhaslach/1051_02_10.cei.xml
- KURaitenhaslach/1079_10_24.cei.xml

Die Trainingsbasis beinhaltet Urkunden aus folgenden Sammlungen:

### Ardagger, Kollegiat (1049-1743) (ArdCan) ###
Edition: Godfrid Edmund Friess, Geschichte des einstigen Collegiat-Stiftes Ardagger in Nieder-Oesterreich. In: AÖG 46 (1871) 419-561.
Monasterium: <https://www.monasterium.net/mom/ArdCan/collection>
- 1049_I_07.cei.xml

### Freising, Bistum und Hochstift (763-1364) (FreisBm) ###
Edition: Joseph von Zahn: Codex diplomaticus Austriaco-Frisingensis : Sammlung von Urkunden und Urbaren zur Geschichte der ehemals Freisingischen Besitzungen in Österreich, Wien 1870.
Monasterium: <https://www.monasterium.net/mom/FreisBm/collection>
- 0965_IV_03.cei.xml
- 0972_V_28.cei.xml
- 0973_VI_3.cei.xml
- 0973_XI_23.cei.xml
- 0989_X_01.cei.xml
- 0993_VII_19.cei.xml
- 1002_XI_11.cei.xml
- 1007_V_10.1.cei.xml
- 1033_V_07.cei.xml
- 1033_VII_19.cei.xml
- 1040_I_18.cei.xml
- 1049_I_0.cei.xml
- 1055_XII_10.cei.xml
- 1067_III_05.cei.xml
- 1074_XI_26.cei.xml
- 1140_V_03.cei.xml
- 1189_V_18.cei.xml

### Oberösterreichisches Urkundenbuch, weltlicher Teil (540-1399) (OOEUB) ###
Edition: Erich Trinks, Hans Sturmberger, Othmar Hegeneder (Bearb.): Urkunden-Buch des Landes ob der Enns, Bde. 1-11, Wien 1852-1983
Monasterium: <https://www.monasterium.net/mom/OOEUB/collection>
- 0976_VII_22.cei.xml
- 0977_X_05.1.cei.xml
- 0993_I_27.cei.xml
- 1005_XII_07.cei.xml
- 1007_XI_01.1.cei.xml
- 1007_XI_01.cei.xml
- 1018.cei.xml
- 1049_VI_16.cei.xml

### St. Pölten, Augustiner Chorherren (976-1668) (StPCanReg) ###
Edition: Josef Lampel, Anton Felel (Bearb.), Niederösterreichisches Urkundenbuch. Acta Austriae inferioris. Urkundenbuch des aufgehobenen Chorherrenstiftes Sanct Pölten (976-1400), Bde. 1-2, Wien 1891-1901
Monasterium: <https://www.monasterium.net/mom/StPCanReg/collection>
- 0976_VII_22.cei.xml
- 1058_X_02.cei.xml

## Änderungen ##
Veränderungen am Code oder an den Trainingsdokumenten im Gegensatz zum in der technischen Dokumentation beschriebenen Zustand:

19.03.2020: Anpassung der Trainingsbasis von 89 Urkunden; 6 Urkunden wurden nachträglich entfernt. Gründe:
- 1 ausgestellt von Friedrich II., dessen Urkunden nicht in das Korpus mit aufgenommen werden sollten
- 2 liegen nicht mehr im gewählten Zeitraum von 932-1214
- 1 Urkunde Ottos II. von 972, die wortwörtlich eine Urkunde seines Vaters kopiert, der diese am selben Tag ausstellte
- 2 Fälschungen bzw. vermutete Fälschungen
-> Daraus folgt, dass das Korpus nun 83 Urkunden enthält.
