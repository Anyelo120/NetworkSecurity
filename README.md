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
## 🤖 Discord Integration (since 2025-07-09)

- 🔐 **2FA Verification via Discord**: Requires players to confirm login from linked Discord account
- 🔗 **Linking System**: Players can link their Minecraft account to Discord via `/vincular-discord <discord_name>`
- ❌ **Unlink Support**: Players can unlink their Discord account with `/desvincular-discord`
- ⏱️ Confirmation messages auto-expire after 5 minutes if no response
- 🧠 **API for developers**:
  - `DiscordLinkAPI.getDiscordIdByMinecraft(UUID)` – Get Discord ID from Minecraft UUID
  - `DiscordLinkAPI.getMinecraftUUIDByDiscordId(String)` – Get Minecraft UUID from Discord ID
  - `DiscordConfirmationAPI.solicitarConfirmacion(UUID)` – Ask Discord user to confirm login
- 🛡️ Admins can monitor login verifications via Discord with interactive buttons (✅ Yes / ❌ No)
- 🌐 Multi-language support for Discord messages

🎬 Demo video: https://youtu.be/mXGepIGXYEg
---

## 🛠 How to Build

### Requirements

- Java 11
- Maven 3.6+
- Git (optional, for cloning)

📜 License
MIT License © RottenBoneStudio
