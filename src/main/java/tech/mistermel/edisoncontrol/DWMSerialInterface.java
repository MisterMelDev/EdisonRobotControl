package tech.mistermel.edisoncontrol;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListenerWithExceptions;

public class DWMSerialInterface {

	private static final Logger logger = LoggerFactory.getLogger(DWMSerialInterface.class);
	
	private SerialPort port;
	
	public void setup() {
		JSONObject configJson = EdisonControl.getInstance().getConfigHandler().getJson().optJSONObject("dwm_serial");
		if(configJson == null) {
			logger.warn("DWM serial port initialization failed, no 'dwm_serial' config section");
			return;
		}
		
		String serialPortName = configJson.optString("port_name", "/dev/ttyACM0");
		int baudrate = configJson.optInt("baudrate", 115200);
		
		this.port = SerialPort.getCommPort(serialPortName);
		port.setBaudRate(baudrate);
		
		if(!port.openPort()) {
			logger.warn("Failed to open DWM serial port ({} at {} baud)", serialPortName, baudrate);
			return;
		}
		logger.info("DWM serial port opened ({} at {} baud)", serialPortName, baudrate);
		
		this.initialize();
		
		port.addDataListener(new SerialPortMessageListenerWithExceptions() {

			@Override
			public void serialEvent(SerialPortEvent event) {
				String str = new String(event.getReceivedData()).trim();
				logger.info(str);
				
				if(str.equals("dwm>")) {
					sendString("lep\r");
					return;
				}
				
				String[] args = str.split(",");
				if(args.length != 5) {
					logger.debug("Received invalid message: {}", str);
					return;
				}
				
				String msgType = args[0];
				if(!msgType.equals("POS")) {
					logger.warn("Received invalid message type: {}", msgType);
					return;
				}
				
				float x = Float.parseFloat(args[1]);
				float y = Float.parseFloat(args[2]);
				EdisonControl.getInstance().getNavHandler().onPositionReceived(x, y);
			}
			
			@Override
			public byte[] getMessageDelimiter() {
				return new byte[] { '\r' };
			}

			@Override
			public boolean delimiterIndicatesEndOfMessage() {
				return true;
			}

			@Override
			public int getListeningEvents() {
				return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
			}

			@Override
			public void catchException(Exception e) {
				logger.error("Error occured in DWM serial interface", e);
			}
			
		});
	}
	
	private void initialize() {
		port.writeBytes(new byte[] { 0x0D, 0x0D }, 2);
	}
	
	private void sendString(String str) {
		logger.debug("Sending: {}", str);
		byte[] bytes = str.getBytes();
		port.writeBytes(bytes, bytes.length);
	}
	
}
