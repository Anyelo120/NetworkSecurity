package rottenbonestudio.system.SecurityNetwork.storage;

import rottenbonestudio.system.SecurityNetwork.common.api.IPAnalysisResult;

public interface StorageProvider {
	void initialize();

	void saveIP(String ip, boolean isBlocked, String country, String continent);

	IPAnalysisResult getCachedAnalysis(String ip);

	boolean isCountryMismatch(String uuid, String currentCountry);
	
	void deleteIP(String ip);

	int countBlockedIPs();

	int countAllowedIPs();

	boolean confirmAndWipe(boolean confirm);
	
    void updatePlayerIP(String uuid, String ip);
    
    String getLastIP(String uuid);

    String getPlayersLinkedToIP(String ip);
}
