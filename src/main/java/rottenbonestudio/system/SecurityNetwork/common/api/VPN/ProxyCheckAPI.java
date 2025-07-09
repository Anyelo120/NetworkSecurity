package rottenbonestudio.system.SecurityNetwork.common.api.VPN;

import org.json.JSONObject;

import rottenbonestudio.system.SecurityNetwork.common.api.IPAnalysisResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProxyCheckAPI {

    private final String apiKey;

    public ProxyCheckAPI(String apiKey) {
        this.apiKey = apiKey;
    }

    public IPAnalysisResult analyzeIP(String ip) {
        try {
            String urlStr = "https://proxycheck.io/v2/" + ip + "?key=" + apiKey + "&risk=1&vpn=1";
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

            if (json.has(ip)) {
                JSONObject ipData = json.getJSONObject(ip);
                boolean isProxy = ipData.optString("proxy", "no").equalsIgnoreCase("yes");

                return new IPAnalysisResult("??", "Unknown", isProxy);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new IPAnalysisResult("??", "Unknown", false);
    }
    
}
