package edu.hm.dako.chat.benchmark;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.hm.dako.chat.common.ChatPDU;
import edu.hm.dako.chat.common.ChatPDUDecoder;
import edu.hm.dako.chat.common.ChatPDUEncoder;
import edu.hm.dako.chat.common.ClientConversationStatus;

/**
 * <p/>
 * Benchmarking-Client: Simuliert einen Chat-User
 *
 * @author Mandl
 */

@ClientEndpoint(encoders = { ChatPDUEncoder.class }, decoders = { ChatPDUDecoder.class })
public class BenchmarkingClientImplSimple extends AbstractChatClient {

	private static Log log = LogFactory.getLog(BenchmarkingClientImplSimple.class);

	/*
	 * Parameter fuer den Benchmarking-Lauf
	 */
	protected int messageLength;
	protected int numberOfMessagesToSend;
	protected int clientThinkTime;

	// Pfad zur Ablage der Ergebnis-Logs
	protected String resultPath;

	// Kennzeichen, ob zuletzt erwartete Chat-Response-PDU des Clients
	// angekommen ist
	private AtomicBoolean chatResponseReceived = new AtomicBoolean();

	// Array zur Ablage aller RTTs für Ergebnis-Log
	private long[] chatPduRtt;
	// Liste der Serverzeiten
	private ArrayList<Long> serverTime;

	/**
	 * Konstruktor fuer Benchmarking
	 * 
	 * @param userInterface
	 *            Schnittstelle zur GUI
	 * @param benchmarkingGui
	 *            Schnittstelle zur BenchmarkingGUI
	 * @param implementationType
	 *            Typ der Implementierung
	 * @param serverPort
	 *            Port des Servers
	 * @param remoteServerAddress
	 *            Hostadresse des Servers
	 * @param numberOfClient
	 *            Anzahl der zu simulierenden Clients
	 * @param messageLength
	 *            Laenge der Chat-Nachrichten
	 * @param numberOfMessages
	 *            Anzahl der Nachrichten pro Client
	 * @param clientThinkTime
	 *            Maximale Denkzeit zwischen zwei Chat-Requests
	 * @param numberOfRetries
	 *            Anzahl Wiederholungen bei Nachrichtenverlust
	 * @param responseTimeout
	 *            Timeout bei Uebrwachung der Bestaetigungen
	 * @param sharedStatistics
	 *            Statistikdaten
	 * @param connectionFactory
	 *            Connection Fabrik
	 * @throws IOException
	 * @throws DeploymentException
	 */
	public BenchmarkingClientImplSimple(URI uri, int messageLength, int numberOfMessages, int clientThinkTime,
			int numberOfClient, String resultPath) {

		this.messageLength = messageLength;
		this.numberOfMessagesToSend = numberOfMessages;
		this.clientThinkTime = clientThinkTime;
		this.chatPduRtt = new long[numberOfMessagesToSend];
		this.serverTime = new ArrayList<>();
		this.resultPath = resultPath;

		Thread.currentThread().setName("Client-Thread-" + numberOfClient);
		threadName = Thread.currentThread().getName();

		WebSocketContainer client = ContainerProvider.getWebSocketContainer();
		try {
			this.session = client.connectToServer(this, uri);
		} catch (DeploymentException | IOException e) {
			log.error(e.getMessage());
		}

	}

	/**
	 * Callback-Methode bei Verbindungsaufbau
	 */
	@OnOpen
	public void onOpen(Session session) {
		log.debug("Session geoeffnet. Wurde durch connectToServer-Methode ausgelöst");
	}

	/**
	 * Callback-Methode bei Verbindungsfehler
	 */
	@OnError
	public void onError(Throwable t) {
		log.error("Kommunikation wurde unerwartet abgebrochen: " + t.getMessage());
	}

	/**
	 * Callback-Methode bei Verbindungsabbau
	 */
	@OnClose
	public void onClose() {
		if (this.status != ClientConversationStatus.UNREGISTERED) {
			try {
				this.logout(threadName);
				session.close();
			} catch (IOException e) {
				log.error(e.getMessage());
			}

		}
		System.exit(1);
	}

	/**
	 * Callback-Methode zur Verarbeitung eintreffender Nachrichten
	 */
	@OnMessage
	public void onMessage(ChatPDU receivedPdu) {
		handleIncomingPdu(receivedPdu);
	}

	/**
	 * User wird beim Server registriert,
	 * alle Requests werden gesendet, Antworten werden gelesen und am Ende wird ein
	 * Logout ausgefuehrt. Der Vorgang wird abprupt abgebrochen, wenn dies ueber die
	 * GUI gewuenscht wird.
	 */
	public void executeTest() {
		try {
			// Login ausfuehren und warten, bis Server bestaetigt
			this.login(threadName);

			while (this.status != ClientConversationStatus.REGISTERED) {

				Thread.sleep(1);
				if (this.status == ClientConversationStatus.UNREGISTERED) {
					// Fehlermeldung vom Server beim Login-Vorgang
					log.debug("User " + userName + " schon im Server angemeldet");
					return;
				}
			}

			log.debug("User " + userName + " beim Server angemeldet");

			// Alle Chat-Nachrichten senden
			int i = 0;
			while (i < numberOfMessagesToSend) {

				log.debug(userName + " sendet die " + i + ". Nachricht - mit Thread: "
						+ Thread.currentThread().getName());

				sendMessageAndWaitForAck(i);

				try {
					// Zufaellige Zeit, aber maximal die angegebene Denkzeit
					// warten
					int randomThinkTime = (int) (Math.random() * clientThinkTime) + 1;
					Thread.sleep(randomThinkTime);
				} catch (Exception e) {
					e.printStackTrace();
				}

				i++;

				log.debug("Gesendete Chat-Nachrichten von " + userName + ": " + i);
			}

			// Logout ausfuehren und warten, bis Server bestaetigt
			this.logout(threadName);

			while (this.status != ClientConversationStatus.UNREGISTERED) {
				Thread.sleep(1);
			}

			// Nachbearbeitung fuer die Statistik
			log.debug("User " + userName + " beim Server abgemeldet");

			printStatistic();

			// Transportverbindung zum Server abbauen
			session.close();

		} catch (Exception e) {
			log.error(e.getMessage());
		}

	}
	/**
	 * Erstellt die Ergebnis-Logs
	 */
	private void printStatistic() {

		Long[] serverTimes = new Long[serverTime.size()];
		serverTimes = serverTime.toArray(serverTimes);

		log.debug("Client bereitet Ausgabe vor");

		Result r = new Result("Simple", userName, eventCounter.get(), messageCounter.get(), loginEvents.get(),
				logoutEvents.get(), chatPduRtt, serverTimes);

		ObjectMapper mapper = new ObjectMapper();
		String jsonInString = "";

		try {
			jsonInString = mapper.writeValueAsString(r);
		} catch (JsonProcessingException e1) {
			e1.printStackTrace();
		}

		Path p = Paths.get(resultPath + userName + ".txt");

		if (!Files.exists(p)) {
			try {
				Files.createFile(p);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			Files.write(p, jsonInString.getBytes(), StandardOpenOption.WRITE);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Warten, bis Server einen Chat-Response als Antwort auf den letzten
	 * Chat-Request gesendet hat (nur fuer Benchmarking)
	 */
	private synchronized void waitUntilChatResponseReceived() {

		while (!chatResponseReceived.getAndSet(false)) {

			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		notifyAll();

	}

	/**
	 * Chat-Nachricht an den Server senden und auf Antwort warten. Methode wird nur
	 * von Benchmarking-Client genutzt
	 * 
	 * @param i
	 *            Nummer des Clients
	 * @throws Exception
	 */
	private void sendMessageAndWaitForAck(int i) throws Exception {

		// Dummy-Nachricht zusammenbauen
		String chatMessage = "";
		for (int j = 0; j < messageLength; j++) {
			chatMessage += "+";
		}

		// Senden der Nachricht und warten, bis Bestaetigung vom Server da ist
		try {

			// RTT-Startzeit ermitteln (ns)
			long rttStartTime = System.nanoTime();

			tell(userName, chatMessage);

			// Warten, bis Chat-Response empfangen wurde, dann erst naechsten
			// Chat Request senden
			waitUntilChatResponseReceived();

			// Response in Statistik aufnehmen
			chatPduRtt[i] = System.nanoTime() - rttStartTime;
			log.debug(userName + ": RTT fuer Nachricht" + i + ": " + chatPduRtt[i]);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected synchronized void chatMessageResponseAction(ChatPDU receivedPdu) {
		log.debug("Sequenznummer der Chat-Response-PDU " + receivedPdu.getUserName() + ": "
				+ receivedPdu.getSequenceNumber() + ", Messagecounter: " + this.messageCounter.get());

		if (receivedPdu.getSequenceNumber() == this.messageCounter.get()) {

			serverTime.add(receivedPdu.getServerTime());
			chatResponseReceived.set(true);
			notifyAll();

			log.debug("Chat-Response-PDU fuer Client " + receivedPdu.getUserName()
					+ " empfangen, Serverbearbeitungszeit: " + +receivedPdu.getServerTime() / 1000000 + " ms");

		} else {
			log.debug("Sequenznummer der Chat-Response-PDU " + receivedPdu.getUserName() + " passt nicht: "
					+ receivedPdu.getSequenceNumber() + "/" + this.messageCounter.get());
		}

	}

	@Override
	protected void chatMessageEventAction(ChatPDU receivedPdu) {
		log.debug("Chat-Message-Event-PDU von " + receivedPdu.getEventUserName() + " empfangen");

		// Eventzaehler erhoehen
		this.eventCounter.getAndIncrement();
		int events = this.messageEvents.incrementAndGet();

		log.debug("MessageEventCounter: " + events);

	}

	@Override
	protected void loginResponseAction(ChatPDU receivedPdu) {
		if (receivedPdu.getErrorCode() == ChatPDU.LOGIN_ERROR) {
			// Login hat nicht funktioniert
			log.debug("Login-Response-PDU fuer Client " + receivedPdu.getUserName() + " mit Login-Error empfangen");
			this.status = ClientConversationStatus.UNREGISTERED;

			// Verbindung wird gleich geschlossen
			try {
				session.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// Login hat funktioniert
			this.status = ClientConversationStatus.REGISTERED;

			Thread.currentThread().setName("Listener" + "-" + userName);
			log.debug("Login-Response-PDU fuer Client " + receivedPdu.getUserName() + " empfangen");
		}
	}

	@Override
	protected void loginEventAction(ChatPDU receivedPdu) {

		// Eventzaehler fuer Testzwecke erhoehen
		this.eventCounter.getAndIncrement();
		int events = loginEvents.incrementAndGet();

		log.debug(this.userName + " erhaelt LoginEvent, LoginEventCounter: " + events);

	}

	@Override
	protected void logoutEventAction(ChatPDU receivedPdu) {
		log.debug(this.userName + " empfaengt Logout-Event-PDU fuer Client " + receivedPdu.getUserName());
		log.debug(this.userName + ": Clientliste: " + receivedPdu.getClients());

		// Eventzaehler fuer Testzwecke erhoehen
		this.eventCounter.getAndIncrement();
		int events = this.logoutEvents.incrementAndGet();

		log.debug("LogoutEventCounter: " + events);

	}

	@Override
	protected void logoutResponseAction(ChatPDU receivedPdu) {
		log.debug(this.userName + " empfaengt Logout-Response-PDU fuer Client " + receivedPdu.getUserName());
		this.status = ClientConversationStatus.UNREGISTERED;

		log.debug("Vom Client gesendete Chat-Nachrichten:  " + this.messageCounter.get());

		finished = true;
	}

	@Override
	public void handleIncomingPdu(ChatPDU receivedPdu) {

		if (receivedPdu != null) {

			switch (this.status) {

			case REGISTERING:

				switch (receivedPdu.getPduType()) {

				case LOGIN_RESPONSE:
					// Login-Bestaetigung vom Server angekommen
					loginResponseAction(receivedPdu);

					break;

				case LOGIN_EVENT:
					// Meldung vom Server, dass sich die Liste der
					// angemeldeten User erweitert hat
					loginEventAction(receivedPdu);

					break;

				case LOGOUT_EVENT:
					// Meldung vom Server, dass sich die Liste der
					// angemeldeten User veraendert hat
					logoutEventAction(receivedPdu);

					break;

				case CHAT_MESSAGE_EVENT:
					// Chat-Nachricht vom Server gesendet
					chatMessageEventAction(receivedPdu);
					break;

				default:
					log.debug("Ankommende PDU im Zustand " + this.status + " wird verworfen");
				}
				break;

			case REGISTERED:

				switch (receivedPdu.getPduType()) {

				case CHAT_MESSAGE_RESPONSE:

					// Die eigene zuletzt gesendete Chat-Nachricht wird vom
					// Server bestaetigt.
					chatMessageResponseAction(receivedPdu);
					break;

				case CHAT_MESSAGE_EVENT:
					// Chat-Nachricht vom Server gesendet
					chatMessageEventAction(receivedPdu);
					break;

				case LOGIN_EVENT:
					// Meldung vom Server, dass sich die Liste der
					// angemeldeten User erweitert hat
					loginEventAction(receivedPdu);

					break;

				case LOGOUT_EVENT:
					// Meldung vom Server, dass sich die Liste der
					// angemeldeten User veraendert hat
					logoutEventAction(receivedPdu);

					break;

				default:
					log.debug("Ankommende PDU im Zustand " + this.status + " wird verworfen");
				}
				break;

			case UNREGISTERING:

				switch (receivedPdu.getPduType()) {

				case CHAT_MESSAGE_EVENT:
					// Chat-Nachricht vom Server gesendet
					chatMessageEventAction(receivedPdu);
					break;

				case LOGOUT_RESPONSE:
					// Bestaetigung des eigenen Logout
					logoutResponseAction(receivedPdu);
					break;

				case LOGIN_EVENT:
					// Meldung vom Server, dass sich die Liste der
					// angemeldeten User erweitert hat
					loginEventAction(receivedPdu);

					break;

				case LOGOUT_EVENT:
					// Meldung vom Server, dass sich die Liste der
					// angemeldeten User veraendert hat
					logoutEventAction(receivedPdu);

					break;

				default:
					log.error(
							"Ankommende PDU im Zustand " + this.status + " wird verworfen" + receivedPdu.getPduType());
					break;
				}
				break;

			case UNREGISTERED:
				log.debug("Ankommende PDU im Zustand " + this.status + " wird verworfen");

				break;

			default:
				log.debug("Unzulaessiger Zustand " + this.status);
			}
		}

	}

}