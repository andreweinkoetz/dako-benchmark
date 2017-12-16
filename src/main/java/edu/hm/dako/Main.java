package edu.hm.dako;

import java.net.URI;

import org.apache.log4j.PropertyConfigurator;

import edu.hm.dako.chat.benchmark.BenchmarkingClientImplAdvanced;
import edu.hm.dako.chat.common.ImplementationType;

/**
 * Hello world!
 *
 */
public class Main {
	public static void main(String[] args) {
//		String log4jConfPath = "log4j.client.properties";
//		PropertyConfigurator.configure(log4jConfPath);
		
		boolean useAdvancedChat = args[0] == "y";
		String serverAdress = args[1];
		int numberOfClient = Integer.parseInt(args[2]);
		int numberOfMessages = Integer.parseInt(args[3]);
		int messageLength = Integer.parseInt(args[4]);
		int clientThinkTime = Integer.parseInt(args[5]);

		URI uri = URI.create("ws://" + serverAdress);

		BenchmarkingClientImplAdvanced client = new BenchmarkingClientImplAdvanced(
				useAdvancedChat ? ImplementationType.TCPAdvancedImplementation
						: ImplementationType.TCPSimpleImplementation,
				uri, messageLength, numberOfMessages, clientThinkTime, numberOfClient);
		client.executeTest();
	}
}
