package edu.hm.dako.chat.benchmark;

/**
 * Repräsentiert das Ergebnis eines Durchlaufs des Benchmark-Clients.
 * Wird als JSON-String in einem Ergebnis-Logfile abgelegt.
 * 
 * @author Andre Weinkötz
 *
 */
public class Result {
	
	// Benchmark-Client
	private String userName;
	// Impl.-Variante
	private String implementation;

	// Anzahl Events gesamt
	private int totalEvents;
	// Anzahl Nachrichten
	private int sendMessages;

	//Anzahl Login/Logout-Events
	private int loginEvents;
	private int logoutEvents;

	// RTTs und Serverzeiten
	private long[] chatPduRtt;
	private Long[] serverTime;

	/**
	 * Benötigt für Enkodierung/Dekodierung
	 */
	public Result() {
	}

	/**
	 * Konstruktor
	 * 
	 * @param implementation
	 * @param userName
	 * @param totalEvents
	 * @param sendMessages
	 * @param loginEvents
	 * @param logoutEvents
	 * @param chatPduRtt
	 * @param serverTime
	 */
	public Result(String implementation, String userName, int totalEvents, int sendMessages, int loginEvents,
			int logoutEvents, long[] chatPduRtt, Long[] serverTime) {
		this.implementation = implementation;
		this.userName = userName;
		this.totalEvents = totalEvents;
		this.sendMessages = sendMessages;
		this.loginEvents = loginEvents;
		this.logoutEvents = logoutEvents;
		this.chatPduRtt = chatPduRtt;
		this.serverTime = serverTime;
		
	}

	public String getImplementation() {
		return implementation;
	}

	public void setImplementation(String implementation) {
		this.implementation = implementation;
	}

	public long[] getChatPduRtt() {
		return chatPduRtt;
	}

	public void setChatPduRtt(long[] chatPduRtt) {
		this.chatPduRtt = chatPduRtt;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public int getTotalEvents() {
		return totalEvents;
	}

	public void setTotalEvents(int totalEvents) {
		this.totalEvents = totalEvents;
	}

	public int getSendMessages() {
		return sendMessages;
	}

	public void setSendMessages(int sendMessages) {
		this.sendMessages = sendMessages;
	}

	public int getLoginEvents() {
		return loginEvents;
	}

	public void setLoginEvents(int loginEvents) {
		this.loginEvents = loginEvents;
	}

	public int getLogoutEvents() {
		return logoutEvents;
	}

	public void setLogoutEvents(int logoutEvents) {
		this.logoutEvents = logoutEvents;
	}

	public Long[] getServerTime() {
		return serverTime;
	}

	public void setServerTime(Long[] serverTime) {
		this.serverTime = serverTime;
	}

}
