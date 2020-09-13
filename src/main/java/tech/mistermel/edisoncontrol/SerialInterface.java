package tech.mistermel.edisoncontrol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListenerWithExceptions;

public class SerialInterface extends Thread {

	private static final String SERIAL_PORT_NAME = "/dev/ttyS0";
	private static final int SERIAL_BAUD = 38400;
	private static final int START_FRAME = 0xABCD;
	private static final int SEND_INTERVAL = 50;
	
	private static final Logger logger = LoggerFactory.getLogger(SerialInterface.class);
	
	private SerialPort port;
	
	private short speed, steer;
	
	private int speedR, speedL;
	private double battVoltage, boardTemp;

	public SerialInterface() {
		super("SerialThread");
	}
	
	@Override
	public void run() {
		this.port = SerialPort.getCommPort(SERIAL_PORT_NAME);
		port.setBaudRate(SERIAL_BAUD);
		
		if(!port.openPort()) {
			logger.warn("Failed to open serial port ({} at {} baud)", SERIAL_PORT_NAME, SERIAL_BAUD);
			return;
		}
		logger.info("Serial port opened ({} at {} baud)", SERIAL_PORT_NAME, SERIAL_BAUD);
		
		port.addDataListener(new SerialPortMessageListenerWithExceptions() {
			
			@Override
			public void serialEvent(SerialPortEvent event) {
				if(event.getReceivedData().length != 18) {
					return;
				}
				
				ByteBuffer buffer = ByteBuffer.wrap(event.getReceivedData());
				buffer.order(ByteOrder.LITTLE_ENDIAN);
				
				int start = toSigned(buffer.getShort());
				if(start != START_FRAME) {
					logger.warn("Received message with incorrect start frame, skipping");
					return;
				}
				
				int cmd1 = (int) buffer.getShort();
				int cmd2 = (int) buffer.getShort();
				int speedR = (int) buffer.getShort();
				int speedL = (int) buffer.getShort();
				int battVoltage = (int) buffer.getShort();
				int boardTemp = (int) buffer.getShort();
				int cmdLed = toSigned(buffer.getShort());
				
				SerialInterface.this.speedR = speedR;
				SerialInterface.this.speedL = speedL;
				SerialInterface.this.battVoltage = (double) battVoltage / 100;
				SerialInterface.this.boardTemp = (double) boardTemp / 10;
				
				//logger.debug("cmd1: {} cmd2: {} speedR: {} speedL: {} battVoltage: {} boardTemp: {} cmdLed: {}", cmd1, cmd2, speedR, speedL, battVoltage, boardTemp, cmdLed);
			}
			
			@Override
			public void catchException(Exception e) {
				e.printStackTrace();
			}
			
			@Override
			public int getListeningEvents() {
				return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
			}
			
			@Override
			public byte[] getMessageDelimiter() {
				return new byte[] { (byte) START_FRAME };
			}
			
			@Override
			public boolean delimiterIndicatesEndOfMessage() {
				return false;
			}
		});
		
		while(true) {
			long nextRun = System.currentTimeMillis() + SEND_INTERVAL;
			
			this.send();
			
			long sleepTime = nextRun - System.currentTimeMillis();
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				break;
			}
		}
		
		logger.warn("SerialInterface send loop exited");
	}
	
	private int toSigned(int unsigned) {
		return unsigned & 0xFFFF;
	}
	
	public void setControls(short speed, short steer) {
		this.speed = speed;
		this.steer = steer;
	}
	
	public void setControls(int speed, int steer) {
		this.speed = (short) speed;
		this.steer = (short) steer;
	}
	
	private void send() {
		short startFrame = (short) toSigned(START_FRAME);
		short checksum = (short) toSigned(startFrame ^ steer ^ speed);
		
		ByteBuffer buf = ByteBuffer.allocate(8);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.putShort(startFrame);
		buf.putShort(steer);
		buf.putShort(speed);
		buf.putShort(checksum);
		
		buf.rewind();
		byte[] bytes = new byte[buf.remaining()];
		buf.get(bytes);

		port.writeBytes(bytes, bytes.length);
	}
	
	public int getSpeedR() {
		return speedR;
	}
	
	public int getSpeedL() {
		return speedL;
	}
	
	public double getBattVoltage() {
		return battVoltage;
	}
	
	public double getBoardTemp() {
		return boardTemp;
	}
	
}
