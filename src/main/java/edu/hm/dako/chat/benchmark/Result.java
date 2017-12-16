package edu.hm.dako.chat.benchmark;

public class Result {
	
	private String userName;
	private String implementation;

	private int totalEvents;
	private int sendMessages;

	private int loginEvents;
	private int logoutEvents;

	private long[] chatPduRtt;

	public Result() {
	}

	public Result(String implementation, String userName, int totalEvents, int sendMessages, int loginEvents,
			int logoutEvents, long[] chatPduRtt) {
		this.implementation = implementation;
		this.userName = userName;
		this.totalEvents = totalEvents;
		this.sendMessages = sendMessages;
		this.loginEvents = loginEvents;
		this.logoutEvents = logoutEvents;
		this.chatPduRtt = chatPduRtt;
		
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

}
