package rottenbonestudio.system.SecurityNetwork.common.api.GEO;

import org.json.JSONObject;
import rottenbonestudio.system.SecurityNetwork.common.api.IPAnalysisResult;
import rottenbonestudio.system.SecurityNetwork.common.utils.CountryContinentResolver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class IpApiAPI {

    public IPAnalysisResult analyzeIP(String ip) {
        try {
            String urlStr = "http://ip-api.com/json/" + ip;
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

            if (!"success".equalsIgnoreCase(json.optString("status", "fail")))
                return new IPAnalysisResult("??", "Unknown", false);

            String countryCode = json.optString("countryCode", "??");
            String continent = CountryContinentResolver.getContinent("PE");
            
            return new IPAnalysisResult(countryCode, continent, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new IPAnalysisResult("??", "Unknown", false);
    }
    
}
