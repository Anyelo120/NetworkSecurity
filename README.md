# SecurityNetwork 🔐

**SecurityNetwork** is a Minecraft cross-platform plugin (Spigot/Paper, BungeeCord, and Velocity) designed to enhance server security by blocking connections from unwanted proxies, countries, or continents. It uses external APIs to analyze player IP addresses and applies customizable rules.

---

## 🌐 Features

- ✅ Supports **Spigot**, **Paper**, **BungeeCord**, and **Velocity**
- 🔍 Detects VPNs and proxies using **ProxyCheck**, **IpInfo**, or **IPGeolocation**
- 🌍 Country and continent allow/deny lists
- 💾 Supports **SQLite** or **MySQL** for caching
- ⚡ Dynamic dependency downloader to keep the jar size minimal
- 🧪 `/ipchecktest <ip>` command to test API integrations

---

## 📁 API
```java
// Get Discord ID from Minecraft UUID
DiscordLinkAPI.getDiscordIdByMinecraft(UUID playerUUID);

// Get Minecraft UUID from Discord ID
DiscordLinkAPI.getMinecraftUUIDByDiscordId(String discordId);
```
```Java
CompletableFuture<Boolean> confirmacion = DiscordConfirmationAPI.solicitarConfirmacionConDetalles(
    playerUUID,
    ip,                  // null o real
    pais,                // null o real
    continente,          // null o real
    horaIntento,         // null o real
    cuentasVinculadas    // null o real
);
```

Ejemplos:

```Java
String ip = "123.45.67.89";
String pais = "AR";
String continente = "South America";
String hora = LocalTime.now().toString();
String cuentas = DATABASE.getPlayersLinkedToIP(ip);

DiscordConfirmationAPI
    .solicitarConfirmacion(playerUUID, ip, pais, continente, hora, cuentas)
    .thenAccept(confirmado -> {
        if (confirmado) {
            // -----------
        } else {
            // -----------
        }
    });
```
```java
DiscordConfirmationAPI
    .solicitarConfirmacion(playerUUID, null, null, null, null, null)
    .thenAccept(confirmado -> {
        if (confirmado) {
            // OK
        }
    });
```

---

## 🛠 How to Build

### Requirements

- Java 11
- Maven 3.6+
- Git (optional, for cloning)

📜 License
MIT License © RottenBoneStudio
