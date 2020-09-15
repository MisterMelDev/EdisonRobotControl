package tech.mistermel.edisoncontrol;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigHandler {

	private static final String FILE_NAME = "config.json";
	private static final Logger logger = LoggerFactory.getLogger(ConfigHandler.class);
	
	private File configFile;
	private JSONObject json;
	
	public ConfigHandler() {
		this.configFile = new File(FILE_NAME);
	}
	
	public void load() {
		Path configPath = configFile.toPath();
		
		if(!configFile.exists()) {
			try {
				InputStream in = this.getClass().getClassLoader().getResourceAsStream(FILE_NAME);
				Files.copy(in, configPath);
			} catch(IOException e) {
				logger.error("Error occurred while attempting to save default configuration", e);
			}
		}
		
		try {
			byte[] encoded = Files.readAllBytes(configPath);
			this.json = new JSONObject(new String(encoded, Charset.defaultCharset()));
		} catch (IOException | JSONException e) {
			logger.error("Error occurred while attempting to read configuration", e);
		}	
	}
	
	public JSONObject getJson() {
		return json;
	}
	
}
