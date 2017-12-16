package edu.hm.dako.chat.benchmark;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
import edu.hm.dako.chat.client.AbstractChatClient;
import edu.hm.dako.chat.common.ChatPDU;
import edu.hm.dako.chat.common.ChatPDUDecoder;
import edu.hm.dako.chat.common.ChatPDUEncoder;
import edu.hm.dako.chat.common.ClientConversationStatus;
import edu.hm.dako.chat.common.ImplementationType;

/**
 * <p/>
 * Benchmarking-Client: Simuliert einen Chat-User
 *
 * @author Mandl
 */

@ClientEndpoint(encoders = { ChatPDUEncoder.class }, decoders = { ChatPDUDecoder.class })
public class BenchmarkingClientImplAdvanced extends AbstractChatClient {

	private static Log log = LogFactory.getLog(BenchmarkingClientImplAdvanced.class);

	/*
	 * Parameter fuer den Benchmarking-Lauf
	 */
	protected int messageLength;
	protected int numberOfMessagesToSend;
	protected int clientThinkTime;

	protected ImplementationType implementationType;

	// Kennzeichen, ob zuletzt erwartete Chat-Response-PDU des Clients
	// angekommen ist
	private AtomicBoolean chatResponseReceived = new AtomicBoolean();

	private long[] chatPduRtt;

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
	public BenchmarkingClientImplAdvanced(ImplementationType implementationType, URI uri, int messageLength,
			int numberOfMessages, int clientThinkTime, int numberOfClient) {

		this.implementationType = implementationType;
		this.messageLength = messageLength;
		this.numberOfMessagesToSend = numberOfMessages;
		this.clientThinkTime = clientThinkTime;
		this.chatPduRtt = new long[numberOfMessagesToSend];

		Thread.currentThread().setName("Client-Thread-" + numberOfClient);
		threadName = Thread.currentThread().getName();

		WebSocketContainer client = ContainerProvider.getWebSocketContainer();
		try {
			this.session = client.connectToServer(this, uri);
		} catch (DeploymentException | IOException e) {
			log.error(e.getMessage());
		}

	}

	@OnOpen
	public void onOpen(Session session) {
		log.debug("Session geoeffnet. Wurde durch connectToServer-Methode ausgelöst");
	}

	@OnError
	public void onError(Throwable t) {
		log.error("Kommunikation wurde unerwartet abgebrochen: " + t.getMessage());
	}

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

	@OnMessage
	public void onMessage(ChatPDU receivedPdu) {
		handleIncomingPdu(receivedPdu);
	}

	/**
	 * Thread zur Simulation eines Chat-Users: User wird beim Server registriert,
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

			// TODO: Nachbearbeitung fuer die Statistik
			log.debug("User " + userName + " beim Server abgemeldet");
			printStatistic();

			// Transportverbindung zum Server abbauen
			session.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void printStatistic() {

		StringBuilder sb = new StringBuilder();
		sb.append(userName + ":\n");
		sb.append("Send messages: " + messageCounter.get());
		sb.append(";Received events total: " + eventCounter.get());
		sb.append(";Received login events: " + loginEvents.get());
		sb.append(";Received logout events: " + logoutEvents.get());
		sb.append("\n");

		for (int i = 0; i < chatPduRtt.length - 1; i++) {
			sb.append(chatPduRtt[i] + ";");
		}

		sb.append(chatPduRtt[chatPduRtt.length - 1] + "\n");

		String workingDir = BenchmarkingClientImplAdvanced.class.getProtectionDomain().getCodeSource().getLocation()
				.getPath();
		workingDir = workingDir.substring(0, workingDir.lastIndexOf('/'));

		Path p = Paths.get(workingDir + "/result.txt");

		if (!Files.exists(p)) {
			try {
				Files.createFile(p);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			Files.write(p, sb.toString().getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// System.out.println("Send messages: " +messageCounter.get());
		// System.out.println("Received events total: " + eventCounter.get());
		// System.out.println("Received login events: " + loginEvents.get());
		// System.out.println("Received logout events: " + logoutEvents.get());
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

		// ADVANCED_CHAT:Chat-Message-Event bestaetigen
		confirmChatMessageEvent(receivedPdu);

		// ADVANCED_CHAT:ConfirmCounter erhoehen
		this.confirmCounter.getAndIncrement();

	}

	/**
	 * ADVANCED_CHAT: Bestaetigung fuer eine Chat-Event-Message-PDU an Server senden
	 * 
	 * @param receivedPdu
	 *            Empfangene Chat-Event-Message-PDU
	 * @throws Exception
	 */
	private void confirmChatMessageEvent(ChatPDU receivedPdu) {

		ChatPDU responsePdu = ChatPDU.createChatMessageEventConfirm(userName, receivedPdu);

		try {
			sendPduToServer(userName, responsePdu);
			log.debug("Chat-Message-Event-Confirm-PDU fuer " + receivedPdu.getUserName()
					+ " bzgl. eines urspruenglichonapaen Events von " + receivedPdu.getEventUserName()
					+ " an den Server gesendet");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ADVANCED_CHAT:Bestaetigung fuer eine Login-Event-PDU an Server senden
	 * 
	 * @param receivedPdu
	 *            Empfangene Chat-Event-Message-PDU
	 * @throws Exception
	 */
	private void confirmLoginEvent(ChatPDU receivedPdu) {

		ChatPDU responsePdu = ChatPDU.createLoginEventConfirm(userName, receivedPdu);

		try {
			sendPduToServer(userName, responsePdu);
			log.debug("Login-Event-Confirm-PDU fuer " + receivedPdu.getUserName()
					+ " bzgl. eines urspruenglichen Events von " + receivedPdu.getEventUserName()
					+ " an den Server gesendet");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ADVANCED_CHAT: Bestaetigung fuer eine Logout-Event-PDU an Server senden
	 * 
	 * @param receivedPdu
	 *            Empfangene Chat-Event-Message-PDU
	 * @throws Exception
	 */
	private void confirmLogoutEvent(ChatPDU receivedPdu) {

		ChatPDU responsePdu = ChatPDU.createLogoutEventConfirm(userName, receivedPdu);

		try {
			sendPduToServer(userName, responsePdu);
			log.debug("Logout-Event-Confirm-PDU fuer " + receivedPdu.getUserName()
					+ " bzgl. eines urspruenglichen Events von " + receivedPdu.getEventUserName()
					+ " an den Server gesendet");
		} catch (Exception e) {
			log.error(e.getMessage());
		}
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

		// ADVANCED_CHAT: Bestaetigung senden
		confirmLoginEvent(receivedPdu);

		// ADVANCED_CHAT:ConfirmCounter erhoehen
		this.confirmCounter.getAndIncrement();

	}

	@Override
	protected void logoutEventAction(ChatPDU receivedPdu) {
		log.debug(this.userName + " empfaengt Logout-Event-PDU fuer Client " + receivedPdu.getUserName());
		log.debug(this.userName + ": Clientliste: " + receivedPdu.getClients());

		// Eventzaehler fuer Testzwecke erhoehen
		this.eventCounter.getAndIncrement();
		int events = this.logoutEvents.incrementAndGet();

		log.debug("LogoutEventCounter: " + events);

		// ADVANCED_CHAT: Bestaetigung senden
		confirmLogoutEvent(receivedPdu);

		// ADVANCED_CHAT: Confirmation-Zaehler erhoehen
		this.confirmCounter.getAndIncrement();
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