package tech.mistermel.edisoncontrol.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;
import tech.mistermel.edisoncontrol.EdisonControl;
import tech.mistermel.edisoncontrol.SerialInterface;

public class WebHandler extends NanoWSD {

	private static final Logger logger = LoggerFactory.getLogger(WebHandler.class);
	
	private static final int PORT = 8888;
	private static final Map<String, String> MIME_TYPES = new HashMap<>();
	private static final String DEFAULT_MIME_TYPE = "text/plain";
	private static final String INDEX_FILE = "index.html";
	
	static {
		MIME_TYPES.put("html", "text/html");
		MIME_TYPES.put("css", "text/css");
		MIME_TYPES.put("js", "text/javascript");
	}
	
	private File staticFolder;
	private WebSocketHandler webSocketHandler;
	
	public WebHandler() {
		super(PORT);
		
		this.staticFolder = new File("static");
		if(!staticFolder.isDirectory()) {
			staticFolder.mkdirs();
		}
	}
	
	public void start() {
		try {
			this.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
			new TelemetryThread().start();
			
			logger.info("Webserver startup completed");
		} catch (IOException e) {
			logger.error("Error occurred while attempting to start webserver", e);
		}
	}
	
	private class TelemetryThread extends Thread {
		
		@Override
		public void run() {
			SerialInterface serialInterface = EdisonControl.getInstance().getSerialInterface();
			while(true) {
				if(webSocketHandler != null) {
					JSONObject json = new JSONObject();
					json.put("type", "telemetry");
					json.put("battVoltage", serialInterface.getBattVoltage());
					json.put("boardTemp", serialInterface.getBoardTemp());
					
					JSONObject speedJson = new JSONObject();
					json.put("speed", speedJson);
					speedJson.put("right", serialInterface.getSpeedR());
					speedJson.put("left", serialInterface.getSpeedL());
					
					try {
						webSocketHandler.send(json.toString());
					} catch (IOException e) {
						logger.error("Error occurred while attempting to send telemetry packet", e);
					}
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
			
			logger.warn("Telemetry packet send loop exited");
		}
		
	}
	
	@Override
	public Response serveHttp(IHTTPSession session) {
		String uri = session.getUri().equals("/") ? INDEX_FILE : session.getUri();
		File file = new File(staticFolder, uri);
		
		if(!file.exists()) {
			return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "The specified file could not be found");
		}
		
		try {
			String mimeType = this.getMimeType(file);
			return newFixedLengthResponse(Response.Status.OK, mimeType, new FileInputStream(file), file.length());
		} catch (FileNotFoundException e) {
			// This will never happen
			e.printStackTrace();
			
			return null;
		}
	}
	
	private String getMimeType(File file) {
		String fileName = file.getName();
		String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
		
		String mimeType = MIME_TYPES.get(extension);
		if(mimeType != null) {
			return mimeType;
		}
		
		return DEFAULT_MIME_TYPE;
	}

	@Override
	protected WebSocket openWebSocket(IHTTPSession handshake) {
		if(webSocketHandler != null) {
			webSocketHandler.disconnectForNewConnection();
		}
		
		WebSocketHandler webSocketHandler = new WebSocketHandler(handshake);
		this.webSocketHandler = webSocketHandler;
		return webSocketHandler;
	}
	
	public void onWebSocketClose() {
		this.webSocketHandler = null;
	}
	
}
