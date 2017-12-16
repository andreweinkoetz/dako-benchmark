#!/bin/bash

if [ "$#" -ne 6 ]; then
	echo ""
	echo "*******************************************************************"
	echo ""
	echo "Nicht die passende Anzahl an Argumenten vergeben"
	echo "Verwendung: "
	echo "PARAM1: Adresse des Servers: 			adress:port/path"
	echo "PARAM2: Anzahl Clients:				Zahl"
	echo "PARAM3: Anzahl Nachrichten:			Zahl"
	echo "PARAM4: Nachrichtenlänge:				Zahl"
	echo "PARAM5: Denkzeit des Clients:			Zahl"
	echo "PARAM6: Benutzen des AdvancedChat:		y/n"
	echo ""
	echo "*******************************************************************"
	echo ""
	exit
fi

SERVER_ADRESS=$1
NUMBER_OF_CLIENTS=$2
NUMBER_OF_MESSAGES=$3
MESSAGE_LENGTH=$4
CLIENT_THINK_TIME=$5
USE_ADV_CHAT=$6

i="1"
while [ $i -le $NUMBER_OF_CLIENTS ]
do

	echo "Client $i gestartet"
	if [ $i -eq $NUMBER_OF_CLIENTS ]; then
		java -cp target/dako-benchmark-1.0-SNAPSHOT.jar edu.hm.dako.Main $USE_ADV_CHAT $SERVER_ADRESS $i $NUMBER_OF_MESSAGES $MESSAGE_LENGTH $CLIENT_THINK_TIME
	else 
		java -cp target/dako-benchmark-1.0-SNAPSHOT.jar edu.hm.dako.Main $USE_ADV_CHAT $SERVER_ADRESS $i $NUMBER_OF_MESSAGES $MESSAGE_LENGTH $CLIENT_THINK_TIME &
	fi	
	((i++))
done

echo "Finished with the whole benchmark process"  
