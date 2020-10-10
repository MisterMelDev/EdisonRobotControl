package tech.mistermel.edisoncontrol.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;
import tech.mistermel.edisoncontrol.EdisonControl;
import tech.mistermel.edisoncontrol.navigation.NavigationHandler;
import tech.mistermel.edisoncontrol.serial.SerialInterface;
import tech.mistermel.edisoncontrol.web.packet.Packet;
import tech.mistermel.edisoncontrol.web.packet.TelemetryPacket;

public class WebHandler extends NanoWSD {

	private static final Logger logger = LoggerFactory.getLogger(WebHandler.class);
	
	private static final Map<String, String> MIME_TYPES = new HashMap<>();
	private static final String DEFAULT_MIME_TYPE = "text/plain";
	private static final String INDEX_FILE = "/index.html";
	private static final byte[] PING_PAYLOAD = "1889BEJANDJKM859".getBytes();
	
	static {
		MIME_TYPES.put("html", "text/html");
		MIME_TYPES.put("css", "text/css");
		MIME_TYPES.put("js", "text/javascript");
	}
	
	private File staticFolder;
	private WebSocketHandler webSocketHandler;
	
	private Map<String, WebRoute> routes = new HashMap<>();
	private Map<String, Class<? extends Packet>> packetTypes = new HashMap<>();
	
	public static interface WebRoute {
		public Response serve(IHTTPSession session);
	}
	
	public WebHandler(int port) {
		super(port);
		
		this.staticFolder = new File("static");
		if(!staticFolder.isDirectory()) {
			staticFolder.mkdirs();
		}
	}
	
	public void registerRoute(String uri, WebRoute route) {
		routes.put(uri, route);
		logger.debug("Route registered: {} to {}", uri, route.getClass().getName());
	}
	
	public void registerPacketType(String packetName, Class<? extends Packet> clazz) {
		packetTypes.put(packetName, clazz);
		logger.debug("Packet type registered: {} to {}", packetName, clazz.getClass().getName());
	}
	
	public void startWeb() {
		try {
			this.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
			new TelemetryThread().start();
			
			logger.info("Webserver started on port {}", this.getListeningPort());
		} catch (IOException e) {
			logger.error("Error occurred while attempting to start webserver", e);
		}
	}
	
	protected void onPacketReceive(JSONObject json) {
		String packetType = json.optString("type");
		
		if(packetType.equals("heartbeat")) {
			webSocketHandler.onHeartbeatReceived();
			return;
		}
		
		logger.warn("Received packet with invalid type ('{}')", packetType);
	}
	
	public void sendPacket(Packet packet) {
		if(webSocketHandler == null) {
			return;
		}
		
		JSONObject json = new JSONObject();
		json.put("type", packet.getPacketName());
		packet.send(json);
		
		try {
			webSocketHandler.send(json.toString());
		} catch (IOException e) {
			logger.error("Error occurred while attempting to send packet", e);
		}
	}
	
	private class TelemetryThread extends Thread {
		
		public TelemetryThread() {
			super("TelemetryThread");
		}
		
		@Override
		public void run() {
			SerialInterface serialInterface = EdisonControl.getInstance().getSerialInterface();
			while(true) {
				if(webSocketHandler != null) {
					if(!webSocketHandler.isCheckboxesSent()) {
						webSocketHandler.sendCheckboxes();
					}
					
					TelemetryPacket packet = new TelemetryPacket(serialInterface.getBattVoltage(), serialInterface.getBoardTemp());
					sendPacket(packet);
					
					try {
						webSocketHandler.ping(PING_PAYLOAD);
					} catch (IOException e) {
						logger.error("Error occurred while attempting to send ping", e);
					}
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
			
			logger.warn("Telemetry packet send loop exited");
		}
		
	}
	
	@Override
	public Response serveHttp(IHTTPSession session) {
		if(session.getUri().startsWith(".")) {
			return newFixedLengthResponse(Response.Status.BAD_REQUEST, DEFAULT_MIME_TYPE, "Requests cannot start with .");
		}
		
		String uri = session.getUri().equals("/") ? INDEX_FILE : session.getUri();
		logger.debug("Received {} request for {}", session.getMethod().name(), uri);
		
		WebRoute route = routes.get(uri);
		if(route != null) {
			return route.serve(session);
		}
		
		return this.serveStatic(uri);
	}
	
	private Response serveStatic(String uri) {
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("static" + uri);
		if(in == null) {
			return newFixedLengthResponse(Response.Status.NOT_FOUND, DEFAULT_MIME_TYPE, "The specified file could not be found");
		}
		
		try {
			String mimeType = this.getMimeType(uri);
			return newFixedLengthResponse(Response.Status.OK, mimeType, in, in.available());
		} catch (IOException e) {
			logger.error("Error occurred while attempting to return file", e);
			return null;
		}
	}
	
	private String getMimeType(String fileName) {
		String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
		
		String mimeType = MIME_TYPES.get(extension.toLowerCase());
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
		
		this.webSocketHandler = new WebSocketHandler(handshake);
		return webSocketHandler;
	}
	
	public void onWebSocketClose() {
		this.webSocketHandler = null;
		
		NavigationHandler navHandler = EdisonControl.getInstance().getNavHandler();
		if(navHandler.isActive()) {
			logger.info("Disabling navigation handler because client disconnected");
			navHandler.setActive(false);
		}
		navHandler.clearWaypoints();
	}
	
	public void onHeartbeatReceive() {
		webSocketHandler.onHeartbeatReceived();
	}
	
}
