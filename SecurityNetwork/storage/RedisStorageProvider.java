package rottenbonestudio.system.SecurityNetwork.storage;

import rottenbonestudio.system.SecurityNetwork.common.api.IPAnalysisResult;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.logging.Logger;

public class RedisStorageProvider implements StorageProvider {

    private final String host;
    private final int port;
    private final String password;
    private JedisPool jedisPool;
    private static final Logger logger = Logger.getLogger("NetworkSecurity");

    public RedisStorageProvider(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
    }

    @Override
    public void initialize() {
        JedisPoolConfig config = new JedisPoolConfig();
        jedisPool = new JedisPool(config, host, port, 2000, password.isEmpty() ? null : password);
        logger.info("[Redis] Initialized connection pool.");
    }

    @Override
    public void saveIP(String ip, boolean isBlocked, String country, String continent) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "ip:" + ip;
            jedis.hset(key, "blocked", String.valueOf(isBlocked));
            jedis.hset(key, "country", country);
            jedis.hset(key, "continent", continent);
        } catch (Exception e) {
            logger.severe("[Redis] Error saving IP: " + e.getMessage());
        }
    }

    @Override
    public IPAnalysisResult getCachedAnalysis(String ip) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "ip:" + ip;
            if (jedis.exists(key)) {
                boolean blocked = Boolean.parseBoolean(jedis.hget(key, "blocked"));
                String country = jedis.hget(key, "country");
                String continent = jedis.hget(key, "continent");
                return new IPAnalysisResult(country, continent, blocked);
            }
        } catch (Exception e) {
            logger.severe("[Redis] Error getting cached IP analysis: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean isCountryMismatch(String uuid, String currentCountry) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "country:" + uuid;
            if (jedis.exists(key)) {
                String savedCountry = jedis.get(key);
                return !savedCountry.equalsIgnoreCase(currentCountry);
            } else {
                jedis.set(key, currentCountry);
                return false;
            }
        } catch (Exception e) {
            logger.severe("[Redis] Error checking country mismatch: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void deleteIP(String ip) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("ip:" + ip);
            logger.info("[Redis] Deleted IP: " + ip);
        } catch (Exception e) {
            logger.severe("[Redis] Error deleting IP: " + e.getMessage());
        }
    }

    @Override
    public int countBlockedIPs() {
        try (Jedis jedis = jedisPool.getResource()) {
            int count = 0;
            for (String key : jedis.keys("ip:*")) {
                if ("true".equalsIgnoreCase(jedis.hget(key, "blocked"))) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            logger.severe("[Redis] Error counting blocked IPs: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public int countAllowedIPs() {
        try (Jedis jedis = jedisPool.getResource()) {
            int count = 0;
            for (String key : jedis.keys("ip:*")) {
                if ("false".equalsIgnoreCase(jedis.hget(key, "blocked"))) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            logger.severe("[Redis] Error counting allowed IPs: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean confirmAndWipe(boolean confirm) {
        if (!confirm) return false;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushDB();
            logger.warning("[Redis] All data wiped successfully.");
            return true;
        } catch (Exception e) {
            logger.severe("[Redis] Error wiping Redis DB: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void updatePlayerIP(String uuid, String ip) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set("lastip:" + uuid, ip);
            jedis.sadd("linked:" + ip, uuid);
        } catch (Exception e) {
            logger.severe("[Redis] Error updating player IP: " + e.getMessage());
        }
    }

    @Override
    public String getLastIP(String uuid) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get("lastip:" + uuid);
        } catch (Exception e) {
            logger.severe("[Redis] Error retrieving last IP: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String getPlayersLinkedToIP(String ip) {
        try (Jedis jedis = jedisPool.getResource()) {
            return String.join(", ", jedis.smembers("linked:" + ip));
        } catch (Exception e) {
            logger.severe("[Redis] Error retrieving players linked to IP: " + e.getMessage());
            return "";
        }
    }
    
}
