package edu.hm.dako;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.hm.dako.chat.benchmark.BenchmarkingClientImplAdvanced;
import edu.hm.dako.chat.benchmark.BenchmarkingClientImplSimple;
import edu.hm.dako.chat.client.AbstractChatClient;
import edu.hm.dako.chat.common.ImplementationType;

/**
 * Hello world!
 *
 */
public class Main {
	
	private static Log log = LogFactory.getLog(Main.class);
	
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
