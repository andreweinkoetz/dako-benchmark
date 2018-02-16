#!/bin/bash
LGREY='\033[0;37m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Set variables to params

SERVER_ADRESS=$1
NUMBER_OF_CLIENTS=$2
NUMBER_OF_MESSAGES=$3
MESSAGE_LENGTH=$4
CLIENT_THINK_TIME=$5
USE_ADV_CHAT=$6
CLIENT_START_NUMBER=$7

# Path where log files shall be stored
LOGS_PATH="./tmp/"

# Start the benchmarking process. All processes run in background. Only last process call blocks in order to finish the script execution along with the benchmark.
rm -rf $LOGS_PATH*
i=$7

while [ $i -le $(($NUMBER_OF_CLIENTS + $(($CLIENT_START_NUMBER-1)))) ]
do
	echo "Client $i gestartet"
	if [ $i -eq $NUMBER_OF_CLIENTS ]; then
		java -Xmx1g -jar dako-benchmark-1.0-SNAPSHOT-spring-boot.jar $USE_ADV_CHAT $SERVER_ADRESS $i $NUMBER_OF_MESSAGES $MESSAGE_LENGTH $CLIENT_THINK_TIME $LOGS_PATH
	else 
		java -Xmx1g -jar dako-benchmark-1.0-SNAPSHOT-spring-boot.jar $USE_ADV_CHAT $SERVER_ADRESS $i $NUMBER_OF_MESSAGES $MESSAGE_LENGTH $CLIENT_THINK_TIME $LOGS_PATH &
	fi	
	((i++))
done

