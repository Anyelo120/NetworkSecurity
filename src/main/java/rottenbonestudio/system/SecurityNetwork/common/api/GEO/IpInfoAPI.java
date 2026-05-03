package rottenbonestudio.system.SecurityNetwork.common.api.GEO;

import org.json.JSONObject;

import rottenbonestudio.system.SecurityNetwork.common.api.IPAnalysisResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class IpInfoAPI {
    
    private final String apiKey;

    public IpInfoAPI(String apiKey) {
        this.apiKey = apiKey;
    }

    public IPAnalysisResult analyzeIP(String ip) {
        try {
            String urlStr = "https://api.ipinfo.io/lite/" + ip + "?token=" + apiKey;
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

            String response = responseBuilder.toString();

            JSONObject json = new JSONObject(response);

            String countryCode = json.optString("country_code", "??");
            String continent = json.optString("continent", "Unknown");

            return new IPAnalysisResult(countryCode, continent, false);
            
        } catch (Exception e) {
            return new IPAnalysisResult("??", "Unknown", false);
        }
    }
}
