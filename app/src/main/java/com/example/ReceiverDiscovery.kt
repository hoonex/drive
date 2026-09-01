package com.example

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.util.Collections
import kotlin.coroutines.coroutineContext
import kotlin.math.min

data class DiscoveredReceiver(
    val name: String,
    val ip: String,
    val port: Int,
)

class ReceiverDiscoveryClient {
    companion object {
        const val DISCOVERY_PORT = 26761
        private const val REQUEST = "PCWHEEL_DISCOVER_V1"
        private const val RESPONSE_PREFIX = "PCWHEEL_RECEIVER_V1"
        private const val MAX_RESPONSE_BYTES = 256
    }

    suspend fun discover(timeoutMs: Long = 900L): List<DiscoveredReceiver> = withContext(Dispatchers.IO) {
        val results = linkedMapOf<String, DiscoveredReceiver>()
        val requestBytes = REQUEST.toByteArray(StandardCharsets.UTF_8)
        val deadlineMs = SystemClock.elapsedRealtime() + timeoutMs.coerceIn(250L, 3_000L)

        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.reuseAddress = true
            socket.receiveBufferSize = 16 * 1024
            socket.sendBufferSize = 16 * 1024

            broadcastTargets().forEach { address ->
                coroutineContext.ensureActive()
                runCatching {
                    socket.send(
                        DatagramPacket(
                            requestBytes,
                            requestBytes.size,
                            address,
                            DISCOVERY_PORT,
                        ),
                    )
                }
            }

            val responseBytes = ByteArray(MAX_RESPONSE_BYTES)
            val responsePacket = DatagramPacket(responseBytes, responseBytes.size)

            while (true) {
                coroutineContext.ensureActive()
                val remainingMs = deadlineMs - SystemClock.elapsedRealtime()
                if (remainingMs <= 0L) break

                socket.soTimeout = min(remainingMs, 150L).toInt().coerceAtLeast(1)
                responsePacket.length = responseBytes.size

                try {
                    socket.receive(responsePacket)
                } catch (_: SocketTimeoutException) {
                    continue
                } catch (_: Exception) {
                    break
                }

                val text = String(
                    responsePacket.data,
                    responsePacket.offset,
                    responsePacket.length,
                    StandardCharsets.UTF_8,
                ).trim()
                val parts = text.split('|', limit = 3)
                if (parts.size != 3 || parts[0] != RESPONSE_PREFIX) continue

                val port = parts[2].toIntOrNull()?.takeIf { it in 1..65535 } ?: continue
                val ip = responsePacket.address?.hostAddress?.substringBefore('%') ?: continue
                val name = parts[1].trim().ifEmpty { "PC" }
                results["$ip:$port"] = DiscoveredReceiver(name = name, ip = ip, port = port)
            }
        }

        results.values.sortedWith(compareBy({ it.name.lowercase() }, { it.ip }))
    }

    private fun broadcastTargets(): Set<InetAddress> {
        val targets = linkedSetOf<InetAddress>()
        runCatching { targets += InetAddress.getByName("255.255.255.255") }

        runCatching {
            Collections.list(NetworkInterface.getNetworkInterfaces()).forEach { networkInterface ->
                if (!networkInterface.isUp || networkInterface.isLoopback) return@forEach
                networkInterface.interfaceAddresses.forEach { interfaceAddress ->
                    interfaceAddress.broadcast?.let(targets::add)
                }
            }
        }
        return targets
    }
}
