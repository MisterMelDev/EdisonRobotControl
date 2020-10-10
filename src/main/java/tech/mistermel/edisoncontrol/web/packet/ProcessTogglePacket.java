package tech.mistermel.edisoncontrol.web.packet;

import org.json.JSONObject;

import tech.mistermel.edisoncontrol.EdisonControl;
import tech.mistermel.edisoncontrol.ProcessHandler;

public class ProcessTogglePacket implements Packet {
	
	public static final String PACKET_NAME = "process_toggle";
	
	@Override
	public void send(JSONObject json) {
		ProcessHandler processHandler = EdisonControl.getInstance().getProcessHandler();
		json.put("stream", processHandler.getStreamProcess() != null);
		json.put("lighting", processHandler.getStreamProcess() != null);
	}

	@Override
	public void receive(JSONObject json) {
		String processType = json.optString("type");
		boolean enabled = json.optBoolean("enabled");
		
		if(processType.equals("stream")) {
			EdisonControl.getInstance().getProcessHandler().setStreamProcess(enabled);
			return;
		}
		
		if(processType.equals("lighting")) {
			EdisonControl.getInstance().getProcessHandler().setLightingProcess(enabled);
			return;
		}
	}

	@Override
	public String getPacketName() {
		return PACKET_NAME;
	}

}
