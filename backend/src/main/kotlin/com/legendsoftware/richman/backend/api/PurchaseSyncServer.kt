package com.legendsoftware.richman.backend.api

import com.legendsoftware.richman.backend.model.EntitlementSnapshot
import com.legendsoftware.richman.backend.model.PurchaseSyncRequest
import com.legendsoftware.richman.backend.model.PurchaseType
import com.legendsoftware.richman.backend.service.EntitlementService
import com.legendsoftware.richman.backend.service.PurchaseSyncService
import com.legendsoftware.richman.backend.service.RtdnService
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.Instant

class PurchaseSyncServer(
    private val port: Int,
    private val purchaseSyncService: PurchaseSyncService,
    private val entitlementService: EntitlementService,
    private val rtdnService: RtdnService,
) {
    private val server: HttpServer = HttpServer.create(InetSocketAddress(port), 0)
    private val appApiKey: String? = System.getenv("RICHMAN_APP_API_KEY")?.trim()?.takeIf { it.isNotBlank() }

    fun start() {
        server.createContext("/health") { exchange ->
            exchange.respond(200, """{"status":"ok"}""")
        }
        server.createContext("/internal/shutdown") { exchange ->
            if (System.getenv("RICHMAN_ENABLE_LOCAL_SHUTDOWN") != "true") {
                exchange.respond(404, """{"error":"not_found"}""")
                return@createContext
            }

            exchange.respond(200, """{"status":"stopping"}""")
            Thread { server.stop(0) }.start()
        }
        server.createContext("/v1/play/purchases:sync") { exchange ->
            if (exchange.requestMethod != "POST") {
                exchange.respond(405, """{"error":"method_not_allowed"}""")
                return@createContext
            }
            if (!exchange.hasValidAppApiKey()) {
                exchange.respond(401, """{"error":"unauthorized"}""")
                return@createContext
            }

            runCatching {
                val body = exchange.requestBody.bufferedReader().use { it.readText() }
                val response = purchaseSyncService.sync(JsonCodec.parsePurchaseSyncRequest(body))
                exchange.respond(200, JsonCodec.entitlementResponse("verified", response.entitlements))
            }.onFailure {
                exchange.respond(400, """{"error":"${it.message?.jsonEscape() ?: "bad_request"}"}""")
            }
        }
        server.createContext("/v1/play/notifications") { exchange ->
            if (exchange.requestMethod != "POST") {
                exchange.respond(405, """{"error":"method_not_allowed"}""")
                return@createContext
            }

            runCatching {
                val body = exchange.requestBody.bufferedReader().use { it.readText() }
                val result = rtdnService.processPubSubPush(body)
                exchange.respond(200, """{"status":"${result.status.name.lowercase()}"}""")
            }.onFailure {
                exchange.respond(400, """{"error":"${it.message?.jsonEscape() ?: "bad_request"}"}""")
            }
        }
        server.createContext("/v1/entitlements/me") { exchange ->
            if (exchange.requestMethod != "GET") {
                exchange.respond(405, """{"error":"method_not_allowed"}""")
                return@createContext
            }

            val userId = exchange.requestURI.query
                ?.split("&")
                ?.mapNotNull {
                    val parts = it.split("=", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }
                ?.firstOrNull { it.first == "userId" }
                ?.second

            if (userId.isNullOrBlank()) {
                exchange.respond(400, """{"error":"userId query parameter is required"}""")
                return@createContext
            }

            exchange.respond(200, JsonCodec.entitlements(entitlementService.snapshotForUser(userId)))
        }
        server.start()
    }

    private fun HttpExchange.hasValidAppApiKey(): Boolean {
        val configuredKey = appApiKey ?: return true
        return requestHeaders.getFirst("X-Richman-Api-Key") == configuredKey
    }
}

private object JsonCodec {
    fun parsePurchaseSyncRequest(json: String): PurchaseSyncRequest {
        val productIds = array("productIds", json)
        return PurchaseSyncRequest(
            userId = string("userId", json) ?: error("userId is required"),
            packageName = string("packageName", json) ?: error("packageName is required"),
            purchaseType = when (string("purchaseType", json)?.lowercase()) {
                "one_time", "one-time", "onetime" -> PurchaseType.ONE_TIME
                "subscription" -> PurchaseType.SUBSCRIPTION
                else -> error("purchaseType must be one_time or subscription")
            },
            productIds = productIds.ifEmpty { error("productIds is required") },
            purchaseToken = string("purchaseToken", json) ?: error("purchaseToken is required"),
            appVersion = string("appVersion", json),
            clientPurchaseTime = string("clientPurchaseTime", json)?.let { Instant.parse(it) },
        )
    }

    fun entitlementResponse(status: String, snapshot: EntitlementSnapshot): String =
        """{"status":"$status","entitlements":${entitlements(snapshot)}}"""

    fun entitlements(snapshot: EntitlementSnapshot): String =
        """{"coins":${snapshot.coins},"premiumTier":${snapshot.premiumTier.jsonString()},"premiumState":"${snapshot.premiumState.name.lowercase()}","premiumExpiresAt":${snapshot.premiumExpiresAt?.toString().jsonString()}}"""

    private fun string(name: String, json: String): String? {
        val pattern = Regex(""""$name"\s*:\s*"([^"]*)"""")
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun array(name: String, json: String): List<String> {
        val pattern = Regex(""""$name"\s*:\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
        val content = pattern.find(json)?.groupValues?.get(1) ?: return emptyList()
        return Regex(""""([^"]+)"""").findAll(content).map { it.groupValues[1] }.toList()
    }
}

private fun HttpExchange.respond(statusCode: Int, body: String) {
    responseHeaders.add("Content-Type", "application/json")
    sendResponseHeaders(statusCode, body.toByteArray(Charsets.UTF_8).size.toLong())
    responseBody.use { it.write(body.toByteArray(Charsets.UTF_8)) }
}

private fun String.jsonEscape(): String = replace("\\", "\\\\").replace("\"", "\\\"")

private fun String?.jsonString(): String = this?.let { """"${it.jsonEscape()}"""" } ?: "null"
