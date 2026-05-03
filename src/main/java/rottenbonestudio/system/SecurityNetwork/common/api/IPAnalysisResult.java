package rottenbonestudio.system.SecurityNetwork.common.api;

public class IPAnalysisResult {
    private final String countryCode;
    private final String continent;
    private final boolean isProxy;

    public IPAnalysisResult(String countryCode, String continent, boolean isProxy) {
        this.countryCode = countryCode;
        this.continent = continent;
        this.isProxy = isProxy;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getContinent() {
        return continent;
    }

    public boolean isProxy() {
        return isProxy;
    }
    
}
