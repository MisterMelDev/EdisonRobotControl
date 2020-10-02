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
import tech.mistermel.edisoncontrol.ProcessHandler;
import tech.mistermel.edisoncontrol.SerialInterface;

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
	
	public void start() {
		try {
			this.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
			new TelemetryThread().start();
			
			logger.info("Webserver started on port {}", this.getListeningPort());
		} catch (IOException e) {
			logger.error("Error occurred while attempting to start webserver", e);
		}
	}
	
	public void onPacketReceive(JSONObject json) {
		String packetType = json.optString("type");
		
		if(packetType.equals("control")) {
			int speed = json.optInt("speed");
			int steer = json.optInt("steer");
			
			if(speed < -1000 || speed > 1000) {
				logger.warn("Received a control packet with a speed value outside of the acceptable range ({})", speed);
				return;
			}
			
			if(steer < -1000 || steer > 1000) {
				logger.warn("Received a control packet with a steer value outside of the acceptable range ({})", steer);
				return;
			}
			
			if(EdisonControl.getInstance().getNavHandler().isActive()) {
				logger.debug("Ignored controls because navigation handler is active");
				return;
			}
			
			EdisonControl.getInstance().getSerialInterface().setControls(speed, steer);
			return;
		}
		
		if(packetType.equals("nav_toggle")) {
			EdisonControl.getInstance().getNavHandler().setActive(json.optBoolean("enabled"));
			return;
		}
		
		if(packetType.equals("lighting")) {
			boolean enabled = json.optBoolean("enabled");
			
			ProcessHandler processHandler = EdisonControl.getInstance().getProcessHandler();
			if(enabled) {
				processHandler.startLightingProcess();
			} else {
				processHandler.stopLightingProcess();
			}
			
			this.sendCheckboxes();
			return;
		}
		
		if(packetType.equals("stream")) {
			boolean enabled = json.optBoolean("enabled");
			
			ProcessHandler processHandler = EdisonControl.getInstance().getProcessHandler();
			if(enabled) {
				processHandler.startStreamProcess();
			} else {
				processHandler.stopStreamProcess();
			}
			
			this.sendCheckboxes();
			return;
		}
		
		if(packetType.equals("heartbeat")) {
			webSocketHandler.onHeartbeatReceived();
			return;
		}
		
		if(packetType.equals("shutdown")) {
			EdisonControl.getInstance().getProcessHandler().shutdown();
			return;
		}
		
		if(packetType.equals("reboot")) {
			EdisonControl.getInstance().getProcessHandler().reboot();
			return;
		}
		
		if(packetType.equals("wifi")) {
			try {
				EdisonControl.getInstance().getWifiHandler().setNetwork(json.optString("ssid"), json.optString("password"));
				EdisonControl.getInstance().getProcessHandler().reboot();
			} catch (IOException e) {
				logger.error("Error occured while attempting to set WiFi network", e);
			}
			
			return;
		}
		
		logger.warn("Received packet with invalid type ('{}')", packetType);
	}
	
	public void sendPacket(JSONObject json) {
		if(webSocketHandler == null) {
			logger.warn("Did not send packet (type: {}) because no websocket handler is connected", json.optString("type"));
			return;
		}
		
		try {
			String jsonStr = json.toString();
			webSocketHandler.send(jsonStr);
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
						sendPosition(0, 0, 0);
					}
					
					JSONObject json = new JSONObject();
					json.put("type", "telemetry");
					json.put("isConnected", serialInterface.isCommunicationWorking());
					json.put("battVoltage", serialInterface.getBattVoltage());
					json.put("boardTemp", serialInterface.getBoardTemp());
					
					JSONObject speedJson = new JSONObject();
					json.put("speed", speedJson);
					speedJson.put("right", serialInterface.getSpeedR());
					speedJson.put("left", serialInterface.getSpeedL());
					
					sendPacket(json);
					
					try {
						webSocketHandler.ping(PING_PAYLOAD);
					} catch (IOException e) {
						logger.error("Error occurred while attempting to send ping", e);
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
	
	public void sendCheckboxes() {
		if(webSocketHandler == null) {
			logger.warn("Could not send checkbox update because no websocket client is connected");
			return;
		}
		
		webSocketHandler.sendCheckboxes();
	}
	
	@Override
	public Response serveHttp(IHTTPSession session) {
		if(session.getUri().startsWith(".")) {
			return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Requests cannot start with .");
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
			return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "The specified file could not be found");
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
		
		WebSocketHandler webSocketHandler = new WebSocketHandler(handshake);
		this.webSocketHandler = webSocketHandler;
		
		return webSocketHandler;
	}
	
	public void onWebSocketClose() {
		this.webSocketHandler = null;
	}

	public void sendPosition(float x, float y, float heading) {
		if(webSocketHandler == null) {
			return;
		}
		
		JSONObject webPacket = new JSONObject();
		webPacket.put("type", "pos");
		webPacket.put("x", x);
		webPacket.put("y", y);
		webPacket.put("h", heading);
		
		this.sendPacket(webPacket);
	}
	
}
