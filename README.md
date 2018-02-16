# DaKo-Benchmark :rocket:
Der Benchmarking-Teil des DaKo-Frameworks. Kann in der simplen und der komplexen Implementierung angewendet werden.

## Bedienung :computer:
Der Benchmark wird als Java-Konsolenanwendung ausgeführt. Dazu werden die ebenfalls in diesem Repository befindlichen Skripte verwendet. Das Skript `start_benchmark.sh` dient der Koordination der Benchmark-Ausführung. Es enthält alle notwendigen Informationen zur Ausführung des Benchmarks auf einer oder mehreren Hostsystemen. Folgende Angaben müssen vor der Ausführung hinterlegt werden:

```
# Path where log files shall be stored
LOGS_PATH="./tmp/"

# Path where log files of clients are stored
LOGS_PATH_CLIENT="tmp/"

# Path of jar-file of log converter
LOG_CONVERTER_JARPATH="/Users/andre/ba-workspace/dako-result/target/dako-result-1.0-SNAPSHOT.jar"

# Add IP addresses as ssh-connections: 'user@ip-adress'
IP_ADRESSES_OF_HOSTS=("user@192.168.1.205" "user@192.168.1.206" "user@192.168.1.207")
```
Das Skript erwartet beim Aufruf die Übergabe einiger Parameter, welche an das Skript `run_client_benchmark.sh`und von diesem an die Anwendung weitergereicht werden. Im Falle unzureichender Angaben wird eine Hilfestellung ausgegeben. Die Parameter sind:
1. Parameter Adresse des Servers:         ipadress:port/path
2. Parameter Anzahl Clients:			        Zahl
3. Parameter Anzahl Nachrichten:	        Zahl
4. Parameter Nachrichtenlänge:		        Zahl	
5. Parameter Denkzeit des Clients:			  Zahl
6. Parameter Benutzen des AdvancedChat:		y/n

## Ablauf :mag:
Durch den Aufruf des Skripts `start_benchmark.sh` wird eine SSH-Verbindung zu den Hosts mit den Benutzern aufgebaut, die in `IP_ADRESSES_OF_HOSTS` hinterlegt sind (Bsp.: admin@172.16.10.2). In dem Home-Verzeichnis des zum Verbindungsaufbau verwendeten Benutzers werden die folgenden Dateien/Verzeichnisse benötigt:

- kompilierte Anwendung `dako-benchmark-1.0-SNAPSHOT-spring-boot.jar`
- Client-Skript `run_client_benchmark.sh`
- Ordner mit Bezeichnung `tmp`

Anschließend wird das Client-Skript ausgeführt, welches die kompilierte Benchmarking-Anwendung startet. Dabei werden soviele Instanzen der Anwendung gestartet, wie zu verwendende Clients im Start-Skript angegeben wurden. Demnach führt die Angabe von 10 Clients bei drei hinterlegten Hosts zum Start von insgesamt 30 Clients.

Wurden alle laufenden Benchmark-Prozesse auf allen Hosts beendet, so kann die Ergebnisauswertung gestartet werden. Dazu muss der Pfad der kompilierten Anwendung des Converters `dako-result-1.0-SNAPSHOT.jar` sowie der Pfad der Einzelergebnisse auf den Hosts (`LOGS_PATH_CLIENT`) angegeben werden. Über das Programm `scp` werden alle Logs auf den Host geladen, auf welchem das Start-Skript ausgeführt wird. Anschließend errechnet der Converter die statistischen Daten des gesamten Laufs und legt sie in einem einzelnen Textfile ab. Beispiel:

```
Ergebnisse des Advanced-Tests vom: 	04.02.2018
Anzahl Clients: 				            15
Insgesamt gesendete Nachrichten: 	  150
Min. RTT: 				                  21 ms
Max. RTT: 				                  2128 ms
Mittlere RTT: 				              682 ms
Mittlere Serverzeit: 			        	625 ms

```

Ein vollständig vorbereiteter virtueller Host befindet sich als VM-Image (Ubuntu 16.04) ebenfalls auf dem USB-Stick der Bachelorarbeit. Dieser muss lediglich importiert und gestartet werden sowie im Netzwerk erreichbar sein, um das Benchmarking auszuführen. 

