package edu.hm.dako.chat;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.hm.dako.chat.benchmark.AbstractChatClient;
import edu.hm.dako.chat.benchmark.BenchmarkingClientImplAdvanced;
import edu.hm.dako.chat.benchmark.BenchmarkingClientImplSimple;
import edu.hm.dako.chat.common.ImplementationType;


/**
 * Main-Klasse
 * 
 * @author Andre Weinkötz
 *
 */
public class Main {
	
	private static Log log = LogFactory.getLog(Main.class);
	
	/**
	 * Main-Methode
	 * 
	 * Param 1: Implementierungstyp: y/n
	 * Param 2: Adresse des Servers (URI)
	 * Param 3: Anzahl Clients
	 * Param 4: Anzahl Nachrichten pro Client
	 * Param 5: Länge einer Nachricht in Byte
	 * Param 6: Denkzeit eines Clients in ms
	 * Param 7: Pfad zur Ablage der Ergebnis-Logs
	 * 
	 * @param args Parameter
	 */
	public static void main(String[] args) {

		ImplementationType type = args[0].equals("y") ? ImplementationType.TCPAdvancedImplementation
				: ImplementationType.TCPSimpleImplementation;
		String serverAdress = args[1];
		int numberOfClient = Integer.parseInt(args[2]);
		int numberOfMessages = Integer.parseInt(args[3]);
		int messageLength = Integer.parseInt(args[4]);
		int clientThinkTime = Integer.parseInt(args[5]);
		String resultPath = args[6];

		URI uri;
		AbstractChatClient client;
		
		if (type == ImplementationType.TCPAdvancedImplementation) {
			uri = URI.create("ws://" + serverAdress + "/advancedchat");
			client = new BenchmarkingClientImplAdvanced(uri, messageLength, numberOfMessages, clientThinkTime,
					numberOfClient, resultPath);
			log.debug("Advanced-Benchmark-Client gestartet");
		} else {
			uri = URI.create("ws://" + serverAdress + "/simplechat");
			client = new BenchmarkingClientImplSimple(uri, messageLength, numberOfMessages, clientThinkTime,
					numberOfClient, resultPath);
			log.debug("Simple-Benchmark-Client gestartet");
		}
		
		log.debug("Benchmark beginnt mit der Ausfuehrung des Tests.");
		
		client.executeTest();
	}
}
