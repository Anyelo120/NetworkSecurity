package rottenbonestudio.system.SecurityNetwork.common.api.VPN;

import org.json.JSONObject;
import rottenbonestudio.system.SecurityNetwork.common.api.IPAnalysisResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class IpHubAPI {

	private final String apiKey;

	public IpHubAPI(String apiKey) {
		this.apiKey = apiKey;
	}

	public IPAnalysisResult analyzeIP(String ip) {
		try {
			String urlStr = "http://v2.api.iphub.info/ip/" + ip;
			HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
			conn.setRequestProperty("X-Key", apiKey);
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);

			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder responseBuilder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				responseBuilder.append(line);
			}
			reader.close();

			JSONObject json = new JSONObject(responseBuilder.toString());

			int block = json.optInt("block", 0);
			boolean isProxy = (block == 1);

			String country = json.optString("countryCode", "??");

			return new IPAnalysisResult(country, "Unknown", isProxy);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new IPAnalysisResult("??", "Unknown", false);
	}
	
}
