# Actualización: HTTP API para consultas seguras (v2.0.2)

Esta actualización añade un servidor HTTP embebido para consultar de forma segura las relaciones entre cuentas de Minecraft (UUID) y Discord (ID). El acceso se protege con un token seguro (`api.token`) generado automáticamente si el archivo de configuración no lo contiene. El puerto del API es configurable.

---

## Resumen rápido

- Base URL: `http://<IP>:<PUERTO>/api/`
- Puerto por defecto: `8080` (configurable en `bot-config.properties` con `api.port`)
- Token seguro: `api.token` (hex generado automáticamente si está vacío)
- Parámetro de autorización: `token` (query param)
- Endpoints:
  - `GET /api/getDiscordIdByMinecraft?uuid=<UUID>&token=<API_TOKEN>`
  - `GET /api/getMinecraftUUIDByDiscordId?discordId=<DISCORD_ID>&token=<API_TOKEN>`

---

## `bot-config.properties` (ejemplo)

```
bot.token=
bot.channelId=
api.token=8f3a2b4c... (hex, auto-generado si está vacío)
api.port=8080
```

Si `api.token` está vacío al iniciar, el plugin generará un token seguro (hex) y lo guardará automáticamente en el archivo.

---

## Endpoints, parámetros y respuestas

### 1) Obtener Discord ID por UUID de Minecraft

- Ruta:  
  `GET /api/getDiscordIdByMinecraft`
- Query params:
  - `uuid` — UUID del jugador Minecraft (ej: `4f5a2b3c-1111-2222-3333-abcdef012345`)
  - `token` — token de API desde `bot-config.properties`
- Respuestas:
  - Éxito (enlace encontrado, HTTP 200):
    ```json
    {
      "success": true,
      "discordId": "123456789012345678"
    }
    ```
  - No link (HTTP 200):
    ```json
    {
      "success": false,
      "error": "Not linked"
    }
    ```
  - Missing param (HTTP 400):
    ```json
    {
      "success": false,
      "error": "Missing uuid parameter"
    }
    ```
  - Token inválido (HTTP 401):
    ```json
    {
      "success": false,
      "error": "Invalid token"
    }
    ```

### 2) Obtener UUID de Minecraft por Discord ID

- Ruta:  
  `GET /api/getMinecraftUUIDByDiscordId`
- Query params:
  - `discordId` — ID de Discord (ej: `123456789012345678`)
  - `token` — token de API
- Respuestas:
  - Éxito (enlace encontrado, HTTP 200):
    ```json
    {
      "success": true,
      "uuid": "4f5a2b3c-1111-2222-3333-abcdef012345"
    }
    ```
  - No link (HTTP 200):
    ```json
    {
      "success": false,
      "error": "Not linked"
    }
    ```
  - Missing param (HTTP 400):
    ```json
    {
      "success": false,
      "error": "Missing discordId parameter"
    }
    ```
  - Token inválido (HTTP 401):
    ```json
    {
      "success": false,
      "error": "Invalid token"
    }
    ```

---

## Códigos HTTP que puedes recibir

- `200 OK` — petición procesada (revisa `success` en el JSON).
- `400 Bad Request` — falta un parámetro obligatorio.
- `401 Unauthorized` — token inválido o ausente.
- `405 Method Not Allowed` — se usó un método distinto a `GET`.

---

## Ejemplos `curl`

Obtener Discord ID por UUID:

```bash
curl -G "http://127.0.0.1:8080/api/getDiscordIdByMinecraft" \
  --data-urlencode "uuid=4f5a2b3c-1111-2222-3333-abcdef012345" \
  --data-urlencode "token=MI_TOKEN_SEGURO"
```

Obtener UUID por Discord ID:

```bash
curl -G "http://127.0.0.1:8080/api/getMinecraftUUIDByDiscordId" \
  --data-urlencode "discordId=123456789012345678" \
  --data-urlencode "token=MI_TOKEN_SEGURO"
```

---