#!/bin/bash
LGREY='\033[0;37m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Path where log files shall be stored
LOGS_PATH="./tmp/"

# Path where log files of clients are stored
LOGS_PATH_CLIENT="tmp/"

# Path of jar-file of log converter
LOG_CONVERTER_JARPATH="/Users/andre/ba-workspace/dako-result/target/dako-result-1.0-SNAPSHOT.jar"

# Add IP addresses as ssh-connections: 'user@ip-adress'
IP_ADRESSES_OF_HOSTS=("andre@192.168.1.205" "andre@192.168.1.206" "andre@192.168.1.207")


if [ "$LOGS_PATH" == "empty" ]; then
	echo ""
	echo "*******************************************************************"
	echo "*								  *"
	echo -e "*		${RED}CONF_ERROR: Bitte Pfad der Logs angeben.${NC}	  *"
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
	echo -e "* ${RED}Nicht die passende Anzahl an Argumenten vergeben${NC}		 *"
	echo -e "* Verwendung:							 *"
	echo -e "* ${LGREY}PARAM1:${NC} Adresse des Servers: 			${BLUE}adress:port/path${NC} *"
	echo -e "* ${LGREY}PARAM2:${NC} Anzahl Clients:			${BLUE}Zahl${NC}		 *"		
	echo -e "* ${LGREY}PARAM3:${NC} Anzahl Nachrichten:			${BLUE}Zahl${NC}		 *"
	echo -e "* ${LGREY}PARAM4:${NC} Nachrichtenlänge:			${BLUE}Zahl${NC}		 *"
	echo -e "* ${LGREY}PARAM5:${NC} Denkzeit des Clients:			${BLUE}Zahl${NC}		 *"
	echo -e "* ${LGREY}PARAM6:${NC} Benutzen des AdvancedChat:		${BLUE}y/n${NC}		 *"
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

CLIENT_SET=1
LENGTH=${#IP_ADRESSES_OF_HOSTS[@]}
for (( i=0; i<${LENGTH}; i++ ));
do
	if [ $i -eq $(($LENGTH-1)) ]; then
		ssh ${IP_ADRESSES_OF_HOSTS[$i]} ./run_client_benchmark.sh $SERVER_ADRESS $NUMBER_OF_CLIENTS $NUMBER_OF_MESSAGES $MESSAGE_LENGTH $CLIENT_THINK_TIME $USE_ADV_CHAT $CLIENT_SET
	else
		ssh ${IP_ADRESSES_OF_HOSTS[$i]} ./run_client_benchmark.sh $SERVER_ADRESS $NUMBER_OF_CLIENTS $NUMBER_OF_MESSAGES $MESSAGE_LENGTH $CLIENT_THINK_TIME $USE_ADV_CHAT $CLIENT_SET &
	fi
        CLIENT_SET=$(($CLIENT_SET + $NUMBER_OF_CLIENTS))
done

echo "Benchmark-Prozess wurde beendet"
echo ""

# Calling converter for results.
echo "Wollen Sie die Ergebnis-Aggregation starten?"
echo -e "Bitte waehlen Sie j/n und bestaetigen mit ${BLUE}[ENTER]${NC}."
read userInput
if [ "$userInput" == "j" ]; then
	for (( i=0; i<${LENGTH}; i++ ));
	do
		scp -r ${IP_ADRESSES_OF_HOSTS[$i]}:$LOGS_PATH_CLIENT ./
	done
	java -cp $LOG_CONVERTER_JARPATH edu.hm.dako.App $LOGS_PATH
	echo "Benchmark-Auswertung vollständig"
fi

echo "Finished"  
