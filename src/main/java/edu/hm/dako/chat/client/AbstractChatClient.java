package edu.hm.dako.chat.client;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.EncodeException;
import javax.websocket.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.hm.dako.chat.common.ChatPDU;
import edu.hm.dako.chat.common.ClientConversationStatus;
import edu.hm.dako.chat.common.PduType;

/**
 * Gemeinsame Funktionalitaet fuer alle Client-Implementierungen.
 * 
 * @author Peter Mandl
 *
 */
public abstract class AbstractChatClient implements ClientCommunication {

	private static Log log = LogFactory.getLog(AbstractChatClient.class);

	protected ClientConversationStatus status;

	// Username (Login-Kennung) des Clients
	protected String userName;

	protected Session session;

	protected String threadName;

	protected AtomicInteger messageCounter = new AtomicInteger(0);
	protected AtomicInteger eventCounter = new AtomicInteger(0);
	protected AtomicInteger messageEvents = new AtomicInteger(0);
	protected AtomicInteger confirmCounter = new AtomicInteger(0);
	protected AtomicInteger loginEvents = new AtomicInteger(0);
	protected AtomicInteger logoutEvents = new AtomicInteger(0);

	protected int localPort;

	protected int serverPort;
	protected String remoteServerAddress;

	// Kennzeichen zum Beenden der Bearbeitung
	protected boolean finished = false;


	@Override
	public void login(String name) throws IOException {

		userName = name;
		this.status = ClientConversationStatus.REGISTERING;
		ChatPDU requestPdu = new ChatPDU();
		requestPdu.setPduType(PduType.LOGIN_REQUEST);
		requestPdu.setClientStatus(this.status);
		requestPdu.setClientThreadName(Thread.currentThread().getName());
		requestPdu.setUserName(userName);

		sendPduToServer(userName, requestPdu);
		log.debug("Login-Request-PDU fuer Client " + userName + " an Server gesendet");

	}

	@Override
	public void logout(String name) throws IOException {

		this.status = ClientConversationStatus.UNREGISTERING;
		ChatPDU requestPdu = new ChatPDU();
		requestPdu.setPduType(PduType.LOGOUT_REQUEST);
		requestPdu.setClientStatus(this.status);
		requestPdu.setClientThreadName(Thread.currentThread().getName());
		requestPdu.setUserName(userName);

		sendPduToServer(userName, requestPdu);

	}

	@Override
	public synchronized void tell(String name, String text) throws IOException {

		ChatPDU requestPdu = new ChatPDU();
		requestPdu.setPduType(PduType.CHAT_MESSAGE_REQUEST);
		requestPdu.setClientStatus(this.status);
		requestPdu.setClientThreadName(Thread.currentThread().getName());
		requestPdu.setUserName(userName);
		requestPdu.setMessage(text);
		requestPdu.setSequenceNumber(messageCounter.incrementAndGet());

		sendPduToServer(userName, requestPdu);

		log.debug("Chat-Message-Request-PDU fuer Client " + name + " an Server gesendet, Inhalt: " + text);
		log.debug("MessageCounter: " + messageCounter.get() + ", SequenceNumber: " + requestPdu.getSequenceNumber());

	}

	@Override
	public boolean isLoggedOut() {
		return (this.status == ClientConversationStatus.UNREGISTERED);
	}

	protected synchronized void sendPduToServer(String client, ChatPDU pdu) {
		try {
			session.getBasicRemote().sendObject(pdu);
		} catch (IOException | EncodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Aktion zur Behandlung ankommender ChatMessageEvents.
	 * 
	 * @param receivedPdu
	 *            Ankommende PDU
	 */
	protected abstract void chatMessageResponseAction(ChatPDU receivedPdu);

	/**
	 * Aktion zur Behandlung ankommender ChatMessageResponses.
	 * 
	 * @param receivedPdu
	 *            Ankommende PDU
	 */
	protected abstract void chatMessageEventAction(ChatPDU receivedPdu);

	/**
	 * Aktion zur Behandlung ankommender Login-Responsesd.
	 * 
	 * @param receivedPdu
	 *            Ankommende PDU
	 */
	protected abstract void loginResponseAction(ChatPDU receivedPdu);

	/**
	 * Aktion zur Behandlung ankommender Login-Events.
	 * 
	 * @param receivedPdu
	 *            Ankommende PDU
	 */
	protected abstract void loginEventAction(ChatPDU receivedPdu);

	/**
	 * Aktion zur Behandlung ankommender Logout-Events.
	 * 
	 * @param receivedPdu
	 *            Ankommende PDU
	 */
	protected abstract void logoutEventAction(ChatPDU receivedPdu);

	/**
	 * Aktion zur Behandlung ankommender Logout-Responses.
	 * 
	 * @param receivedPdu
	 *            Ankommende PDU
	 */
	protected abstract void logoutResponseAction(ChatPDU receivedPdu);
}