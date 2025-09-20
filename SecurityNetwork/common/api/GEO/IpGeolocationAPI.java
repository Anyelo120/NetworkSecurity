package rottenbonestudio.system.SecurityNetwork.common.api.GEO;

import org.json.JSONObject;

import rottenbonestudio.system.SecurityNetwork.common.api.IPAnalysisResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class IpGeolocationAPI {

	private final String apiKey;

	public IpGeolocationAPI(String apiKey) {
		this.apiKey = apiKey;
	}

	public IPAnalysisResult analyzeIP(String ip) {
		try {
			String urlStr = "https://api.ipgeolocation.io/v2/ipgeo?apiKey=" + apiKey + "&ip=" + ip
					+ "&excludes=currency,network";

			HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);

			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String response = reader.readLine();
			reader.close();

			JSONObject json = new JSONObject(response);
			JSONObject location = json.optJSONObject("location");

			String countryCode = location != null ? location.optString("country_code2", "??") : "??";
			String continent = location != null ? location.optString("continent_name", "Unknown") : "Unknown";

			return new IPAnalysisResult(countryCode, continent, false);

		} catch (Exception e) {
			return new IPAnalysisResult("??", "Unknown", false);
		}
	}

}
