package eu.wdaqua.qanary.tagmedisambiguate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.net.ssl.HttpsURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import com.google.gson.*;

public class TagMeRequest {

		private final String url = "https://wat.d4science.org/wat/tag/tag";
		private HashMap<String, String> parameters;
		
		public TagMeRequest(String API_TOKEN) {
			this.parameters = new HashMap<String, String>();
			
			this.parameters.put("lang", "en");
			this.parameters.put("gcube-token", API_TOKEN);
		}
		
		public TagMeResponse doRequest(String text) throws MalformedURLException, IOException {
			
			URL url = new URL(this.url+"?"+getArguments(text));
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
			con.setRequestMethod("GET");

			con.setRequestProperty("Accept", "application/json");
			
			@SuppressWarnings("unused")
			int status = con.getResponseCode();
			
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			String content = "";
			while ((inputLine = in.readLine()) != null) {
			    content += inputLine;
			}
			in.close();
			
			con.disconnect();
			
			Gson gson = new Gson();
		
			TagMeResponse response = gson.fromJson(content, TagMeResponse.class);
			
			return response;
			
		}
		
		private String getArguments(String text) {
			
			StringBuilder stb = new StringBuilder();
			
			for(char c: text.toCharArray()) {
				if(c != ' ')
					stb.append(c);
				else
					stb.append("%20");
			}
			
			parameters.put("text", stb.toString());
			
			return "lang="+parameters.get("lang")+"&gcube-token="+parameters.get("gcube-token")+"&text="+parameters.get("text");
		}
}
