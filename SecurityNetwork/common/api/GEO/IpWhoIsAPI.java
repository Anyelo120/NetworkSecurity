package rottenbonestudio.system.SecurityNetwork.common.api.GEO;

import org.json.JSONObject;
import rottenbonestudio.system.SecurityNetwork.common.api.IPAnalysisResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class IpWhoIsAPI {

	public IPAnalysisResult analyzeIP(String ip) {
		try {
			String urlStr = "https://ipwho.is/" + ip;
			HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
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
			boolean success = json.optBoolean("success", false);
			if (!success)
				return new IPAnalysisResult("??", "Unknown", false);

			String countryCode = json.optString("country_code", "??");
			String continent = json.optString("continent", "Unknown");

			return new IPAnalysisResult(countryCode, continent, false);
		} catch (Exception e) {
			e.printStackTrace();
			return new IPAnalysisResult("??", "Unknown", false);
		}
	}

}
