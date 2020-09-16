package tech.mistermel.edisoncontrol;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessHandler {

	private static Logger logger = LoggerFactory.getLogger(ProcessHandler.class);
	
	private Process streamProcess;
	
	public boolean startStreamProcess() {
		File folder = new File("mjpg-streamer");
		if(!folder.isDirectory()) {
			logger.warn("Cannot start stream process, folder 'mjpg-streamer' does not exist");
			return false;
		}
		
		String streamCmd = EdisonControl.getInstance().getConfigHandler().getJson().optString("stream_cmd");
		
		try {
			this.streamProcess = new ProcessBuilder(streamCmd.split(" "))
					.directory(folder)
					.inheritIO()
					.start();
			
			return true;
		} catch (IOException e) {
			logger.error("Error while attempting to start streaming process", e);
			return false;
		}
	}
	
	public Process getStreamProcess() {
		return streamProcess;
	}

	public void stopStreamingProcess() {
		if(streamProcess != null) {
			streamProcess.destroyForcibly();
		}
	}
	
}
