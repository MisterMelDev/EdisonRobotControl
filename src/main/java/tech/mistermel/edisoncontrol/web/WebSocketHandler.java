package tech.mistermel.edisoncontrol.web;

import java.io.IOException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoWSD.WebSocket;
import fi.iki.elonen.NanoWSD.WebSocketFrame;
import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode;
import tech.mistermel.edisoncontrol.EdisonControl;
import tech.mistermel.edisoncontrol.ProcessHandler;

public class WebSocketHandler extends WebSocket {

	private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
	
	private long lastHeartbeat;
	private boolean checkboxesSent;
	
	public WebSocketHandler(IHTTPSession handshakeRequest) {
		super(handshakeRequest);
	}
	
	public void sendCheckboxes() {
		ProcessHandler processHandler = EdisonControl.getInstance().getProcessHandler();
		JSONObject json = new JSONObject();
		json.put("type", "checkboxes");
		json.put("isLightingEnabled", processHandler.getLightingProcess() != null);
		json.put("isStreamEnabled", processHandler.getStreamProcess() != null);
		
		try {
			this.send(json.toString());
		} catch (IOException e) {
			logger.error("Error occurred while attempting to send packet", e);
		}
		
		this.checkboxesSent = true;
	}
	
	public boolean isCheckboxesSent() {
		return checkboxesSent;
	}

	@Override
	protected void onOpen() {
		logger.info("Websocket connected");
	}

	@Override
	protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
		logger.info("Websocket connection closed (code: {}, reason: {}, initiatedByRemote: {})", code.name(), reason, initiatedByRemote);
		EdisonControl.getInstance().getWebHandler().onWebSocketClose();
	}

	@Override
	protected void onMessage(WebSocketFrame message) {
		try {
			EdisonControl.getInstance().getWebHandler().onPacketReceive(new JSONObject(message.getTextPayload()));
		} catch(Exception e) {
			// When this is not used, the WebSocket connection is terminated whenever an exception occurs
			// and the exception is not logged. Therefore we need to catch the exception and log it manually.
			logger.error("Error occured while attempting to process packet", e);
		}
	}

	@Override
	protected void onPong(WebSocketFrame pong) {
		
	}

	@Override
	protected void onException(IOException exception) {
		logger.error("Error occured in websocket handler", exception);
	}
	
	protected void onHeartbeatReceived() {
		this.lastHeartbeat = System.currentTimeMillis();
	}
	
	public long getLastHeartbeat() {
		return lastHeartbeat;
	}
	
	public void disconnectForNewConnection() {
		try {
			this.close(CloseCode.GoingAway, "Another connection has been opened", false);
		} catch (IOException e) {
			logger.error("Error occured while attempting to close connection", e);
		}
	}
	
}