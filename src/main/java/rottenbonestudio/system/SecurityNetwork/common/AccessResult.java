package rottenbonestudio.system.SecurityNetwork.common;

public enum AccessResult {
    ALLOWED,              // Acceso normal
    BLOCKED_DISCORD,      // Requiere confirmación en Discord
    BLOCKED_SECURITY,     // VPN, Proxy, País, Continente
    BLOCKED_TEMP          // Bloqueo temporal por no confirmar
}
