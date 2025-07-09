package rottenbonestudio.system.SecurityNetwork.common.api.VPN;

import org.json.JSONObject;
import rottenbonestudio.system.SecurityNetwork.common.api.IPAnalysisResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class IPQualityScoreAPI {

    private final String apiKey;

    public IPQualityScoreAPI(String apiKey) {
        this.apiKey = apiKey;
    }

    public IPAnalysisResult analyzeIP(String ip) {
        try {
            String urlStr = "https://ipqualityscore.com/api/json/ip/" + apiKey + "/" + ip;
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
            boolean isProxy = json.optBoolean("proxy", false) || json.optBoolean("vpn", false) || json.optBoolean("tor", false);
            String countryCode = json.optString("country_code", "??");

            return new IPAnalysisResult(countryCode, "Unknown", isProxy);
        } catch (Exception e) {
            e.printStackTrace();
            return new IPAnalysisResult("??", "Unknown", false);
        }
    }
    
}
