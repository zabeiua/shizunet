package com.example.shizukuproxy

object ProfileGenerator {
    /**
     * Produces high-fidelity custom-shaped setup profiles mirroring the Web client configs
     */
    fun generateJson(ip: String, port: Int, geoip: String, finalOutbound: String): String {
        val cleanGeoip = geoip.lowercase().trim()
        val cleanFinal = finalOutbound.lowercase().trim()
        
        return """{
  "log": {
    "level": "info",
    "timestamp": true
  },
  "dns": {
    "servers": [
      {
        "tag": "dns-remote",
        "address": "8.8.8.8",
        "detour": "proxy"
      },
      {
        "tag": "dns-direct",
        "address": "1.1.1.1",
        "detour": "direct"
      }
    ],
    "rules": [
      {
        "geosite": ["$cleanGeoip"],
        "server": "dns-direct"
      }
    ],
    "final": "dns-remote"
  },
  "inbounds": [
    {
      "type": "mixed",
      "tag": "mixed-in",
      "listen": "127.0.0.1",
      "listen_port": 2080
    }
  ],
  "outbounds": [
    {
      "type": "http",
      "tag": "proxy",
      "server": "$ip",
      "server_port": $port
    },
    {
      "type": "direct",
      "tag": "direct"
    },
    {
      "type": "block",
      "tag": "block"
    }
  ],
  "route": {
    "rules": [
      {
        "geoip": ["$cleanGeoip"],
        "outbound": "direct"
      },
      {
        "geosite": ["$cleanGeoip"],
        "outbound": "direct"
      }
    ],
    "final": "$cleanFinal"
  }
}""".trimIndent()
    }
}
