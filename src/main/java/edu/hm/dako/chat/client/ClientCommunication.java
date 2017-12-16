package edu.hm.dako.chat.client;

import java.io.IOException;

import javax.websocket.ClientEndpoint;
import edu.hm.dako.chat.common.ChatPDU;

/**
 * Interface zur Kommunikation des Chat-Clients mit dem Chat-Server
 * 
 * @author Peter Mandl
 *
 */
@ClientEndpoint
public interface ClientCommunication {

	/**
	 * Login-Request an den Server senden
	 * 
	 * @param name
	 *          Username (Login-Kennung)
	 * @throws Exception
	 */
	public void login(String name) throws IOException;

	/**
	 * Logout-Request an den Server senden
	 * 
	 * @param name
	 *          Username (Login-Kennung)
	 * @throws Exception
	 */
	public void logout(String name) throws IOException;

	/**
	 * Senden einer Chat-Nachricht zur Verteilung an den Server
	 * 
	 * @param name
	 *          Username (Login-Kennung)
	 * @param text
	 *          Chat-Nachricht
	 */
	public void tell(String name, String text) throws IOException;

	/**
	 * Pruefen, ob Logout schon komplett vollzogen
	 * 
	 * @return boolean - true = Logout abgeschlossen
	 */
	public boolean isLoggedOut();
	
	public void handleIncomingPdu(ChatPDU receivedPdu);
}
