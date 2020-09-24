package tech.mistermel.edisoncontrol;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessHandler {

	private static Logger logger = LoggerFactory.getLogger(ProcessHandler.class);
	
	private Process streamProcess;
	
	private File scriptsFolder;
	private Map<String, Process> lightingProcesses = new HashMap<>();
	
	public ProcessHandler() {
		this.scriptsFolder = new File("lighting-scripts");
		if(!scriptsFolder.isDirectory()) {
			scriptsFolder.mkdir();
		}
	}
	
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
	
	public void setLightingStatus(String id, boolean enabled) {
		id = id.toLowerCase();
		
		if(enabled) {
			if(lightingProcesses.get(id) != null) {
				logger.warn("Cannot enable lighting '{}', already enabled", id);
				return;
			}
			
			File scriptFile = new File(scriptsFolder, id + ".py");
			if(!scriptFile.exists()) {
				logger.warn("Cannot enable lighting '{}', file 'lightingScripts/{}.py' does not exist", id, id);
				return;
			}
			
			try {
				Process process = Runtime.getRuntime().exec("sudo python3 " + scriptFile.getName());
				lightingProcesses.put(id, process);
				
				logger.debug("Lighting '{}' started", id);
			} catch (IOException e) {
				logger.error("Error while attempting to start lighting", e);
			}
		} else {
			Process process = lightingProcesses.get(id);
			if(process == null) {
				logger.warn("Cannot disable lighting '{}', not enabled", id);
				return;
			}
			
			process.destroyForcibly();
			logger.debug("Lighting '{}' stopped", id);
		}
	}
	
	public boolean isLightingEnabled(String id) {
		return lightingProcesses.get(id) != null;
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
