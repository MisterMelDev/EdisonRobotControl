package tech.mistermel.edisoncontrol.web.packet;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.EdisonControl;

public class ControlPacket implements Packet {

	private static final Logger logger = LoggerFactory.getLogger(ControlPacket.class);
	public static final String PACKET_NAME = "control";
	
	@Override
	public void send(JSONObject json) {
		// This packet is incoming only
	}

	@Override
	public void receive(JSONObject json) {
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
	}

	@Override
	public String getPacketName() {
		return PACKET_NAME;
	}

}
