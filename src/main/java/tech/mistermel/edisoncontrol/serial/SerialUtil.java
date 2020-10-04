package tech.mistermel.edisoncontrol.serial;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;

import tech.mistermel.edisoncontrol.EdisonControl;

public class SerialUtil {

	private SerialUtil() {}
	
	private static final Logger logger = LoggerFactory.getLogger(SerialUtil.class);
	
	public static SerialPort openSerial(String configSection, String defaultPort, int defaultBaudrate) {
		JSONObject configJson = EdisonControl.getInstance().getConfigHandler().getJson().optJSONObject(configSection);
		if(configJson == null) {
			logger.warn("Serial port initialization failed, no '{}' config section", configSection);
			return null;
		}
		
		String serialPortName = configJson.optString("port_name", defaultPort);
		int baudrate = configJson.optInt("baudrate", defaultBaudrate);
		
		SerialPort port = SerialPort.getCommPort(serialPortName);
		port.setBaudRate(baudrate);
		
		if(!port.openPort()) {
			logger.warn("Failed to open serial port ({}: {} at {} baud)", configSection, serialPortName, baudrate);
			return null;
		}
		logger.info("Serial port opened ({}: {} at {} baud)", configSection, serialPortName, baudrate);
		
		return port;
	}
	
}
