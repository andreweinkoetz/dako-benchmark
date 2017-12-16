#!/bin/bash

LOGS_PATH="/Users/andre/ba-workspace/dako-benchmark/target/"
LOG_CONVERTER_JARPATH="/Users/andre/ba-workspace/dako-result/target/dako-result-1.0-SNAPSHOT.jar"


if [ "$LOGS_PATH" == "empty" ]; then
	echo ""
	echo "*******************************************************************"
	echo "*								  *"
	echo "*		CONF_ERROR: Bitte Pfad der Logs angeben.	  *"
	echo "*								  *"
	echo "*******************************************************************"
	echo ""
	exit
fi

# Show help on error
if [ "$#" -ne 6 ]; then
	echo ""
	echo "******************************************************************"
	echo "*								 *"
	echo "* Nicht die passende Anzahl an Argumenten vergeben		 *"
	echo "* Verwendung:							 *"
	echo "* PARAM1: Adresse des Servers: 			adress:port/path *"
	echo "* PARAM2: Anzahl Clients:			Zahl		 *"		
	echo "* PARAM3: Anzahl Nachrichten:			Zahl		 *"
	echo "* PARAM4: Nachrichtenlänge:			Zahl		 *"
	echo "* PARAM5: Denkzeit des Clients:			Zahl		 *"
	echo "* PARAM6: Benutzen des AdvancedChat:		y/n		 *"
	echo "*								 *"
	echo "******************************************************************"
	echo ""
	exit
fi

# Set variables to params

SERVER_ADRESS=$1
NUMBER_OF_CLIENTS=$2
NUMBER_OF_MESSAGES=$3
MESSAGE_LENGTH=$4
CLIENT_THINK_TIME=$5
USE_ADV_CHAT=$6


# Clear logs directory from previous runs.
echo "Alte Log-Dateien werden bereinigt."
find "$LOGS_PATH" -name "*.txt" -delete

# Start the benchmarking process. All processes run in background. Only last process call blocks in order to finish the script execution along with the benchmark.
echo "Benchmark-Prozess startet:"
i="1"
while [ $i -le $NUMBER_OF_CLIENTS ]
do

	echo "Client $i gestartet"
	if [ $i -eq $NUMBER_OF_CLIENTS ]; then
		java -cp target/dako-benchmark-1.0-SNAPSHOT.jar edu.hm.dako.Main $USE_ADV_CHAT $SERVER_ADRESS $i $NUMBER_OF_MESSAGES $MESSAGE_LENGTH $CLIENT_THINK_TIME $LOGS_PATH
	else 
		java -cp target/dako-benchmark-1.0-SNAPSHOT.jar edu.hm.dako.Main $USE_ADV_CHAT $SERVER_ADRESS $i $NUMBER_OF_MESSAGES $MESSAGE_LENGTH $CLIENT_THINK_TIME $LOGS_PATH &
	fi	
	((i++))
done
echo "Benchmark-Prozess wurde beendet"
echo ""

# Calling converter for results, result-files have to be on a network share.
echo "Wollen Sie die Ergebnis-Aggregation starten?"
echo "Beachten Sie: Nur eine Instanz der Aggregation kann gestartet werden."
echo "Bitte waehlen Sie j/n und bestaetigen mit [ENTER]."
read userInput
if [ "$userInput" == "j" ]; then
	java -cp $LOG_CONVERTER_JARPATH edu.hm.dako.App $LOGS_PATH
	echo "Benchmark-Auswertung vollständig"
fi

echo "Finished"  
