package tech.mistermel.edisoncontrol.web;

import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import tech.mistermel.edisoncontrol.EdisonControl;
import tech.mistermel.edisoncontrol.web.WebHandler.WebRoute;

public class WiFiConfigurationsRoute implements WebRoute {

	@Override
	public Response serve(IHTTPSession session) {
		JSONObject json = new JSONObject();
		json.put("configurations", EdisonControl.getInstance().getWifiHandler().getConfigurations());
		
		return WebHandler.newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
	}

}
