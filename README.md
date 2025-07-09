# SecurityNetwork 🔐

**SecurityNetwork** is a Minecraft cross-platform plugin (Spigot/Paper, BungeeCord, and Velocity) designed to enhance server security by blocking connections from unwanted proxies, countries, or continents. It uses external APIs to analyze player IP addresses and applies customizable rules.

---

## 🌐 Features

- ✅ Supports **Spigot/Paper**, **BungeeCord**, and **Velocity**
- 🔍 Detects VPNs and proxies using **ProxyCheck**, **IpInfo**, or **IPGeolocation**
- 🌍 Country and continent allow/deny lists
- 💾 Supports **SQLite** or **MySQL** for caching
- ⚡ Dynamic dependency downloader to keep the jar size minimal
- 🧪 `/ipchecktest <ip>` command to test API integrations

---

## 📁 Project Structure
SecurityNetwork/
├── common/ # Shared logic and APIs
├── spigot/ # Spigot/Paper-specific implementation
├── bungee/ # BungeeCord-specific implementation
├── velocity/ # Velocity-specific implementation
├── storage/ # Storage implementations (MySQL, SQLite)
├── config/ # YAML config loaders
└── libs/ # Auto-downloaded dependencies (not in repo)

---

## 🛠 How to Build

### Requirements

- Java 11
- Maven 3.6+
- Git (optional, for cloning)

📜 License
MIT License © RottenBoneStudio