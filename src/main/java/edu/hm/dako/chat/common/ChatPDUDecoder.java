package edu.hm.dako.chat.common;

import java.io.IOException;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatPDUDecoder implements Decoder.Text<ChatPDU> {

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(EndpointConfig arg0) {
		
	}

	@Override
	public synchronized ChatPDU decode(String arg0) throws DecodeException {
		ChatPDU pdu = null;
		ObjectMapper objectMapper = new ObjectMapper();
		//System.out.println(arg0);
		if(willDecode(arg0)) {
			try {
				pdu = objectMapper.readValue(arg0, ChatPDU.class);
			} catch (JsonParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return pdu;
	}

	@Override
	public boolean willDecode(String arg0) {
		if(arg0.isEmpty()) {
			return false;
		}
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.canDeserialize(objectMapper.constructType(ChatPDU.class));
	}

}
