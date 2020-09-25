package tech.mistermel.edisoncontrol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WiFiHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(WiFiHandler.class);
	
	public void setNetwork(String ssid, String password) throws IOException {
		this.setNetwork(ssid, password, EdisonControl.getInstance().getConfigHandler().getJson().getString("wifi_country_code"));
	}
	
	public void setNetwork(String ssid, String password, String countryCode) throws IOException {
		String text = readInternalFile()
				.replace("{{COUNTRY}}", countryCode)
				.replace("{{SSID}}", ssid)
				.replace("{{PASS}}", password);
		
		File outputFile = new File("/etc/wpa_supplicant/wpa_supplicant.conf");
		outputFile.createNewFile();
		
		FileWriter writer = new FileWriter(outputFile);
		writer.write(text);
		writer.close();
		
		logger.info("WiFi info updated (countryCode: {}, ssid: {}, password: {})", countryCode, ssid, password);
	}
	
	private String readInternalFile() throws IOException {
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("wpa_supplicant.conf");
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		
		StringBuilder builder = new StringBuilder();
		String line;
		while((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append("\n");
		}
		
		in.close();
		reader.close();
		return builder.toString();
	}
	
}
