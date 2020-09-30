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
		this.port = SerialPort.getCommPort("COM12");
		port.setBaudRate(115200);
		
		if(!port.openPort()) {
			logger.warn("Failed to open port");
			return;
		}
		this.initialize();
		
		logger.info("Port opened");
		
		port.addDataListener(new SerialPortMessageListenerWithExceptions() {

			@Override
			public void serialEvent(SerialPortEvent event) {
				String str = new String(event.getReceivedData()).trim();
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
				float z = Float.parseFloat(args[3]);
				
				EdisonControl.getInstance().getWebHandler().sendPosition(x, y, z);
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
		port.writeBytes(new byte[] { '\r', '\r' }, 2);
		this.sendString("lep");
	}
	
	private void sendString(String str) {
		byte[] bytes = str.getBytes();
		port.writeBytes(bytes, bytes.length);
	}
	
}
