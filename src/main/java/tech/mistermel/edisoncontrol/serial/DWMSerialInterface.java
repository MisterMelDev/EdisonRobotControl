package tech.mistermel.edisoncontrol.serial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListenerWithExceptions;

import tech.mistermel.edisoncontrol.EdisonControl;
import tech.mistermel.edisoncontrol.SystemHealthHandler.HealthStatusType;
import tech.mistermel.edisoncontrol.SystemHealthHandler.Service;

public class DWMSerialInterface extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(DWMSerialInterface.class);
	
	private static final int MAX_RECEIVE_INTERVAL = 400;
	
	private SerialPort port;
	private long lastMessage;
	
	public DWMSerialInterface() {
		super("DWMSerialThread");
	}
	
	@Override
	public void run() {
		EdisonControl.setStatus(Service.SERIAL_DWM, HealthStatusType.INITIALIZING);
		
		this.port = SerialUtil.openSerial("dwm_serial", "/dev/ttyACM0", 115200);
		if(port == null) {
			EdisonControl.setStatus(Service.SERIAL_DWM, HealthStatusType.FAULT, "Init failed");
			return;
		}
		
		port.addDataListener(new SerialPortMessageListenerWithExceptions() {

			@Override
			public void serialEvent(SerialPortEvent event) {
				String str = new String(event.getReceivedData()).trim();
				
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
				
				lastMessage = System.currentTimeMillis();
				
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
		
		try {
			this.initialize();
			
			EdisonControl.setStatus(Service.SERIAL_DWM, HealthStatusType.RUNNING);
			logger.info("DWM communication initialized");
		} catch (InterruptedException e) {
			logger.error("Interrupted while attempting to initialize DWM serial port", e);
			Thread.currentThread().interrupt();
		}
	}
	
	private void initialize() throws InterruptedException {
		port.writeBytes(new byte[] { 0x0D, 0x0D }, 2);
		Thread.sleep(1000); // This is quite hacky and should be changed, but eh, it works
		port.writeBytes(new byte[] { 0x0D }, 1);
	}
	
	private void sendString(String str) {
		logger.debug("Sending: {}", str.trim());
		byte[] bytes = str.getBytes();
		port.writeBytes(bytes, bytes.length);
	}
	
	public boolean isCommunicationWorking() {
		return System.currentTimeMillis() - lastMessage < MAX_RECEIVE_INTERVAL; 
	}
	
}
