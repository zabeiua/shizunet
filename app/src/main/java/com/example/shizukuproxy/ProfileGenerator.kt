package com.example.shizukuproxy

object ProfileGenerator {
    /**
     * Produces high-fidelity custom-shaped setup profiles mirroring the Web client configs
     */
    fun generateJson(ip: String, port: Int, geoip: String, finalOutbound: String): String {
        val cleanGeoip = geoip.lowercase().trim()
        val cleanFinal = finalOutbound.lowercase().trim()
        
        return """{
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
        "geoip": "$cleanGeoip",
        "outbound": "direct"
      },
      {
        "geosite": "$cleanGeoip",
        "outbound": "direct"
      }
    ],
    "final": "$cleanFinal"
  }
}""".trimIndent()
    }
}
