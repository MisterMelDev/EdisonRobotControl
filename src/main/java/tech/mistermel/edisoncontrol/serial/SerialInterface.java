package tech.mistermel.edisoncontrol.serial;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListenerWithExceptions;

import tech.mistermel.edisoncontrol.EdisonControl;
import tech.mistermel.edisoncontrol.SystemHealthHandler.HealthStatus;
import tech.mistermel.edisoncontrol.SystemHealthHandler.Service;

public class SerialInterface extends Thread {

	private static final int START_FRAME = 0xABCD;
	private static final int SEND_INTERVAL = 50;
	private static final int MAX_RECEIVE_INTERVAL = 300;
	
	private static final Logger logger = LoggerFactory.getLogger(SerialInterface.class);
	
	private SerialPort port;
	private long lastMessage;
	
	private short speed, steer;
	
	private int speedR, speedL;
	private double battVoltage, boardTemp;

	public SerialInterface() {
		super("SerialThread");
	}
	
	@Override
	public void run() {
		EdisonControl.setStatus(Service.SERIAL_MOBO, HealthStatus.INITIALIZING);
		
		this.port = SerialUtil.openSerial("motherboard_serial", "/dev/ttyS0", 38400);
		if(port == null) {
			EdisonControl.setStatus(Service.SERIAL_MOBO, HealthStatus.FAULT);
			return;
		}
		
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
				
				SerialInterface.this.lastMessage = System.currentTimeMillis();
				
				buffer.position(buffer.position() + 4);
				speedR = (int) buffer.getShort();
				speedL = (int) buffer.getShort();
				battVoltage = ((double) buffer.getShort()) / 100;
				boardTemp = ((double) buffer.getShort()) / 10;
			}
			
			@Override
			public void catchException(Exception e) {
				logger.error("Error occurred in serial interface", e);
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
		
		EdisonControl.setStatus(Service.SERIAL_MOBO, HealthStatus.RUNNING);
		
		while(true) {
			long nextRun = System.currentTimeMillis() + SEND_INTERVAL;
			
			this.send();
			
			long sleepTime = nextRun - System.currentTimeMillis();
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		
		logger.warn("SerialInterface send loop exited");
	}
	
	public boolean isCommunicationWorking() {
		return System.currentTimeMillis() - lastMessage < MAX_RECEIVE_INTERVAL;
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
