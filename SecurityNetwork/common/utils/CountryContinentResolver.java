package rottenbonestudio.system.SecurityNetwork.common.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CountryContinentResolver {

	private static final Map<String, String> COUNTRY_TO_CONTINENT;

	static {
		Map<String, String> map = new HashMap<>();

		// 🌍 North America
		map.put("AG", "North America");
		map.put("AI", "North America");
		map.put("AW", "North America");
		map.put("BB", "North America");
		map.put("BL", "North America");
		map.put("BM", "North America");
		map.put("BQ", "North America");
		map.put("BS", "North America");
		map.put("BZ", "North America");
		map.put("CA", "North America");
		map.put("CR", "North America");
		map.put("CU", "North America");
		map.put("CW", "North America");
		map.put("DM", "North America");
		map.put("DO", "North America");
		map.put("GD", "North America");
		map.put("GL", "North America");
		map.put("GP", "North America");
		map.put("GT", "North America");
		map.put("HN", "North America");
		map.put("HT", "North America");
		map.put("JM", "North America");
		map.put("KN", "North America");
		map.put("LC", "North America");
		map.put("MF", "North America");
		map.put("MQ", "North America");
		map.put("MS", "North America");
		map.put("MX", "North America");
		map.put("NI", "North America");
		map.put("PA", "North America");
		map.put("PR", "North America");
		map.put("SV", "North America");
		map.put("SX", "North America");
		map.put("TC", "North America");
		map.put("TT", "North America");
		map.put("US", "North America");
		map.put("VC", "North America");
		map.put("VG", "North America");
		map.put("VI", "North America");

		// 🌎 South America
		map.put("AR", "South America");
		map.put("BO", "South America");
		map.put("BR", "South America");
		map.put("CL", "South America");
		map.put("CO", "South America");
		map.put("EC", "South America");
		map.put("GY", "South America");
		map.put("PE", "South America");
		map.put("PY", "South America");
		map.put("SR", "South America");
		map.put("UY", "South America");
		map.put("VE", "South America");

		// 🌍 Europe
		map.put("AD", "Europe");
		map.put("AL", "Europe");
		map.put("AT", "Europe");
		map.put("BA", "Europe");
		map.put("BE", "Europe");
		map.put("BG", "Europe");
		map.put("BY", "Europe");
		map.put("CH", "Europe");
		map.put("CY", "Europe");
		map.put("CZ", "Europe");
		map.put("DE", "Europe");
		map.put("DK", "Europe");
		map.put("EE", "Europe");
		map.put("ES", "Europe");
		map.put("FI", "Europe");
		map.put("FR", "Europe");
		map.put("GB", "Europe");
		map.put("GR", "Europe");
		map.put("HR", "Europe");
		map.put("HU", "Europe");
		map.put("IE", "Europe");
		map.put("IS", "Europe");
		map.put("IT", "Europe");
		map.put("LI", "Europe");
		map.put("LT", "Europe");
		map.put("LU", "Europe");
		map.put("LV", "Europe");
		map.put("MC", "Europe");
		map.put("MD", "Europe");
		map.put("ME", "Europe");
		map.put("MK", "Europe");
		map.put("MT", "Europe");
		map.put("NL", "Europe");
		map.put("NO", "Europe");
		map.put("PL", "Europe");
		map.put("PT", "Europe");
		map.put("RO", "Europe");
		map.put("RS", "Europe");
		map.put("RU", "Europe");
		map.put("SE", "Europe");
		map.put("SI", "Europe");
		map.put("SK", "Europe");
		map.put("SM", "Europe");
		map.put("UA", "Europe");
		map.put("VA", "Europe");

		// 🌍 Asia
		map.put("AE", "Asia");
		map.put("AF", "Asia");
		map.put("AM", "Asia");
		map.put("AZ", "Asia");
		map.put("BD", "Asia");
		map.put("BH", "Asia");
		map.put("BN", "Asia");
		map.put("BT", "Asia");
		map.put("CN", "Asia");
		map.put("GE", "Asia");
		map.put("ID", "Asia");
		map.put("IL", "Asia");
		map.put("IN", "Asia");
		map.put("IQ", "Asia");
		map.put("IR", "Asia");
		map.put("JO", "Asia");
		map.put("JP", "Asia");
		map.put("KG", "Asia");
		map.put("KH", "Asia");
		map.put("KP", "Asia");
		map.put("KR", "Asia");
		map.put("KW", "Asia");
		map.put("KZ", "Asia");
		map.put("LA", "Asia");
		map.put("LB", "Asia");
		map.put("LK", "Asia");
		map.put("MM", "Asia");
		map.put("MN", "Asia");
		map.put("MV", "Asia");
		map.put("MY", "Asia");
		map.put("NP", "Asia");
		map.put("OM", "Asia");
		map.put("PH", "Asia");
		map.put("PK", "Asia");
		map.put("PS", "Asia");
		map.put("QA", "Asia");
		map.put("SA", "Asia");
		map.put("SG", "Asia");
		map.put("SY", "Asia");
		map.put("TH", "Asia");
		map.put("TJ", "Asia");
		map.put("TL", "Asia");
		map.put("TM", "Asia");
		map.put("TR", "Asia");
		map.put("TW", "Asia");
		map.put("UZ", "Asia");
		map.put("VN", "Asia");
		map.put("YE", "Asia");

		// 🌍 Africa
		map.put("AO", "Africa");
		map.put("BF", "Africa");
		map.put("BI", "Africa");
		map.put("BJ", "Africa");
		map.put("BW", "Africa");
		map.put("CD", "Africa");
		map.put("CF", "Africa");
		map.put("CI", "Africa");
		map.put("CM", "Africa");
		map.put("CV", "Africa");
		map.put("DJ", "Africa");
		map.put("DZ", "Africa");
		map.put("EG", "Africa");
		map.put("EH", "Africa");
		map.put("ER", "Africa");
		map.put("ET", "Africa");
		map.put("GA", "Africa");
		map.put("GH", "Africa");
		map.put("GM", "Africa");
		map.put("GN", "Africa");
		map.put("GQ", "Africa");
		map.put("GW", "Africa");
		map.put("KE", "Africa");
		map.put("KM", "Africa");
		map.put("LR", "Africa");
		map.put("LS", "Africa");
		map.put("LY", "Africa");
		map.put("MA", "Africa");
		map.put("MG", "Africa");
		map.put("ML", "Africa");
		map.put("MR", "Africa");
		map.put("MU", "Africa");
		map.put("MW", "Africa");
		map.put("MZ", "Africa");
		map.put("NA", "Africa");
		map.put("NE", "Africa");
		map.put("NG", "Africa");
		map.put("RW", "Africa");
		map.put("SC", "Africa");
		map.put("SD", "Africa");
		map.put("SL", "Africa");
		map.put("SN", "Africa");
		map.put("SO", "Africa");
		map.put("SS", "Africa");
		map.put("ST", "Africa");
		map.put("SZ", "Africa");
		map.put("TD", "Africa");
		map.put("TG", "Africa");
		map.put("TN", "Africa");
		map.put("TZ", "Africa");
		map.put("UG", "Africa");
		map.put("ZA", "Africa");
		map.put("ZM", "Africa");
		map.put("ZW", "Africa");

		// 🌏 Oceania
		map.put("AS", "Oceania");
		map.put("AU", "Oceania");
		map.put("CK", "Oceania");
		map.put("FJ", "Oceania");
		map.put("FM", "Oceania");
		map.put("GU", "Oceania");
		map.put("KI", "Oceania");
		map.put("MH", "Oceania");
		map.put("MP", "Oceania");
		map.put("NC", "Oceania");
		map.put("NF", "Oceania");
		map.put("NR", "Oceania");
		map.put("NU", "Oceania");
		map.put("NZ", "Oceania");
		map.put("PG", "Oceania");
		map.put("PN", "Oceania");
		map.put("PW", "Oceania");
		map.put("SB", "Oceania");
		map.put("TK", "Oceania");
		map.put("TO", "Oceania");
		map.put("TV", "Oceania");
		map.put("VU", "Oceania");
		map.put("WF", "Oceania");
		map.put("WS", "Oceania");

		// Final build
		COUNTRY_TO_CONTINENT = Collections.unmodifiableMap(map);
	}

	public static String getContinent(String countryCode) {
		if (countryCode == null)
			return "Unknown";
		return COUNTRY_TO_CONTINENT.getOrDefault(countryCode.toUpperCase(), "Unknown");
	}

}
