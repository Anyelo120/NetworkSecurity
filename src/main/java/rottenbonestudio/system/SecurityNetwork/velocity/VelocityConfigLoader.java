package rottenbonestudio.system.SecurityNetwork.velocity;

import rottenbonestudio.system.SecurityNetwork.common.IpCheckConfig;
import rottenbonestudio.system.SecurityNetwork.common.config.SecurityConfigLoader;

import java.io.IOException;
import java.nio.file.Path;

public class VelocityConfigLoader {

	private final Path configPath;

	public VelocityConfigLoader(Path configPath) {
		this.configPath = configPath;
	}

	public IpCheckConfig load() throws IOException {
		return SecurityConfigLoader.load(configPath);
	}
}
