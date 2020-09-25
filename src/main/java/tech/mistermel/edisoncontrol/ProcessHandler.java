package tech.mistermel.edisoncontrol;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessHandler {

	private static Logger logger = LoggerFactory.getLogger(ProcessHandler.class);
	
	private Process streamProcess;
	private Process lightingProcess;
	
	public boolean startStreamProcess() {
		File folder = new File("mjpg-streamer");
		if(!folder.isDirectory()) {
			logger.warn("Cannot start stream process, folder 'mjpg-streamer' does not exist");
			return false;
		}
		
		JSONObject streamJson = EdisonControl.getInstance().getConfigHandler().getJson().getJSONObject("stream");
		JSONObject resolutionJson = streamJson.getJSONObject("resolution");
		
		int resolutionX = resolutionJson.optInt("x", 1280);
		int resolutionY = resolutionJson.optInt("y", 720);
		int fps = streamJson.optInt("fps", 20);
		
		try {
			ProcessBuilder builder = new ProcessBuilder("./mjpg_streamer", "-o", "output_http.so -w ./www", "-i", "input_raspicam.so -x " + resolutionX + " -y " + resolutionY + " -fps " + fps)
					.directory(folder);
			
			if(logger.isDebugEnabled()) {
				builder.inheritIO();
			}
			
			this.streamProcess = builder.start();
			logger.info("Stream process started");
			
			return true;
		} catch (IOException e) {
			logger.error("Error while attempting to start streaming process", e);
			return false;
		}
	}
	
	public void stopStreamingProcess() {
		if(streamProcess != null) {
			streamProcess.destroyForcibly();
		}
	}
	
	public Process getStreamProcess() {
		return streamProcess;
	}
	
	public void startLightingProcess() {
		File file = new File("lighting-script.py");
		if(!file.exists()) {
			logger.warn("Cannot enable lighting, lighting-script.py does not exist");
			return;
		}
		
		ProcessBuilder builder = new ProcessBuilder("sudo", "python3", file.getAbsolutePath());
		if(logger.isDebugEnabled()) {
			builder.inheritIO();
		}
		
		try {
			this.lightingProcess = builder.start();
		} catch (IOException e) {
			logger.error("Error occurred while attempting to start lighting process");
		}
	}
	
	public void stopLightingProcess() {
		if(lightingProcess != null) {
			lightingProcess.destroyForcibly();
		}
	}
	
	public Process getLightingProcess() {
		return lightingProcess;
	}

	public void shutdown() {
		try {
			logger.info("Shutting down RPi");
			Runtime.getRuntime().exec("sudo shutdown now").waitFor();
		} catch (InterruptedException | IOException e) {
			logger.error("Error while attempting to shut down RPi", e);
		}
	}
	
	public void reboot() {
		try {
			logger.info("Rebooting RPi");
			Runtime.getRuntime().exec("sudo reboot").waitFor();
		} catch (InterruptedException | IOException e) {
			logger.error("Error while attempting to reboot RPi", e);
		}
	}
	
}
