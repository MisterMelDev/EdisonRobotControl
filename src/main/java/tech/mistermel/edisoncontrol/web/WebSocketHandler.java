package tech.mistermel.edisoncontrol.web;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoWSD.WebSocket;
import fi.iki.elonen.NanoWSD.WebSocketFrame;
import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode;
import tech.mistermel.edisoncontrol.EdisonControl;

public class WebSocketHandler extends WebSocket {

	private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
	
	public WebSocketHandler(IHTTPSession handshakeRequest) {
		super(handshakeRequest);
	}

	@Override
	protected void onOpen() {
		logger.info("Websocket connected from {}", this.getHandshakeRequest().getRemoteIpAddress());
	}

	@Override
	protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
		logger.info("Websocket connection closed (code: {}, reason: {}, initiatedByRemote: {})", code.name(), reason, initiatedByRemote);
		EdisonControl.getInstance().getWebHandler().onWebSocketClose();
	}

	@Override
	protected void onMessage(WebSocketFrame message) {
		
	}

	@Override
	protected void onPong(WebSocketFrame pong) {
		
	}

	@Override
	protected void onException(IOException exception) {
		logger.error("Error occured in websocket handler", exception);
	}
	
	public void disconnectForNewConnection() {
		try {
			this.close(CloseCode.GoingAway, "Another conenction has been opened", false);
		} catch (IOException e) {
			logger.error("Error occured while attempting to close connection", e);
		}
	}
	
}