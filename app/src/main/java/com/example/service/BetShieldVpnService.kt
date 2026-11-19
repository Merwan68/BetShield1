package com.example.service

import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.example.BetShieldApplication
import com.example.data.model.BlockEventSource
import com.example.data.model.BlockEventType
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Local-only DNS filtering VpnService.
 * Routes only virtual DNS traffic (10.120.0.2:53) through the TUN interface so regular
 * HTTPS/TCP connections remain untouched with zero TLS/certificate interception.
 */
class BetShieldVpnService : VpnService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var tunInterface: ParcelFileDescriptor? = null
  private var packetLoopJob: Job? = null
  private val isRunning = AtomicBoolean(false)
  private val recentDomainEventTimestamps = ConcurrentHashMap<String, Long>()
  private var networkCallback: ConnectivityManager.NetworkCallback? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        stopProtection(paused = true)
        return START_NOT_STICKY
      }
      ACTION_START, null -> {
        startProtection()
      }
    }
    return START_STICKY
  }

  private fun startProtection() {
    if (isRunning.getAndSet(true)) {
      return
    }
    ServiceStatusBridge.setVpnState(VpnProtectionState.STARTING)

    val matcher = ServiceStatusBridge.activeMatcher.get()
    val notification =
      ProtectionNotificationHelper.buildVpnForegroundNotification(this, matcher.activeDomainCount)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(
        ProtectionNotificationHelper.VPN_NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
      )
    } else {
      startForeground(ProtectionNotificationHelper.VPN_NOTIFICATION_ID, notification)
    }

    if (!establishTun()) {
      isRunning.set(false)
      ServiceStatusBridge.setVpnState(
        VpnProtectionState.ERROR,
        "Unable to open local DNS VPN interface. Check VPN permissions.",
      )
      stopSelf()
      return
    }

    registerNetworkRecoveryCallback()
    ServiceStatusBridge.setVpnState(VpnProtectionState.ACTIVE)
    launchPacketLoop()
  }

  private fun establishTun(): Boolean {
    return runCatching {
        tunInterface?.close()
        val builder =
          Builder()
            .setSession("BetShield Local DNS")
            .addAddress(VPN_CLIENT_IP, 32)
            .addRoute(VPN_DNS_IP, 32)
            .addDnsServer(VPN_DNS_IP)
            .setMtu(1500)
        // Exclude BetShield itself so remote DB updates never loop
        runCatching { builder.addDisallowedApplication(packageName) }
        tunInterface = builder.establish()
        tunInterface != null
      }
      .getOrDefault(false)
  }

  private fun launchPacketLoop() {
    packetLoopJob?.cancel()
    packetLoopJob =
      serviceScope.launch {
        val fd = tunInterface ?: return@launch
        val input = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val buffer = ByteArray(32767)

        while (isRunning.get()) {
          val readBytes =
            runCatching { input.read(buffer) }
              .getOrElse {
                if (isRunning.get()) {
                  ServiceStatusBridge.setVpnState(
                    VpnProtectionState.ERROR,
                    "TUN read interrupted; awaiting network recovery.",
                  )
                }
                break
              }
          if (readBytes <= 0) continue

          val parsed = DnsPacketParser.parseIpv4UdpDns(buffer, readBytes) ?: continue
          val domain = parsed.queriedDomain
          if (domain != null) {
            val match = ServiceStatusBridge.activeMatcher.get().matchDomain(domain)
            if (match != null) {
              val nxPayload = DnsPacketParser.buildNxDomainDnsPayload(parsed.dnsPayload)
              val replyPacket = DnsPacketParser.buildIpv4UdpResponsePacket(parsed, nxPayload)
              synchronized(output) { output.write(replyPacket) }
              ServiceStatusBridge.incrementBlockedQueries()
              recordBlockedDomain(domain, match.category, match.platformName)
              continue
            }
          }

          // Forward non-blocked DNS queries safely via protected UDP socket
          val upstreamReply = forwardDnsQuery(parsed.dnsPayload)
          if (upstreamReply != null) {
            val replyPacket = DnsPacketParser.buildIpv4UdpResponsePacket(parsed, upstreamReply)
            synchronized(output) { output.write(replyPacket) }
            ServiceStatusBridge.incrementForwardedQueries()
          }
        }
      }
  }

  private fun forwardDnsQuery(dnsPayload: ByteArray): ByteArray? {
    for (upstream in UPSTREAM_DNS_SERVERS) {
      val result =
        runCatching {
            DatagramSocket().use { socket ->
              if (!protect(socket)) return@use null
              socket.soTimeout = 2500
              val address = InetAddress.getByName(upstream)
              val outPacket = DatagramPacket(dnsPayload, dnsPayload.size, address, 53)
              socket.send(outPacket)

              val responseBuf = ByteArray(4096)
              val inPacket = DatagramPacket(responseBuf, responseBuf.size)
              socket.receive(inPacket)
              responseBuf.copyOfRange(0, inPacket.length)
            }
          }
          .getOrNull()
      if (result != null) return result
    }
    return null
  }

  private fun recordBlockedDomain(domain: String, category: String, platformName: String) {
    val now = System.currentTimeMillis()
    val lastLogged = recentDomainEventTimestamps[domain] ?: 0L
    // Debounce burst DNS retries (e.g. A + AAAA + HTTPS queries within 4 seconds)
    if (now - lastLogged < 4000L) return
    recentDomainEventTimestamps[domain] = now

    serviceScope.launch {
      val app = applicationContext as? BetShieldApplication ?: return@launch
      app.container.protectionRepository.recordBlockEvent(
        target = domain,
        type = BlockEventType.DOMAIN,
        category = category,
        source = BlockEventSource.VPN_DNS,
      )
      val notificationsEnabled =
        app.container.preferencesRepository.settingsFlow.first().notificationsEnabled
      if (notificationsEnabled) {
        ProtectionNotificationHelper.showBlockAlert(
          this@BetShieldVpnService,
          "Gambling site blocked",
          "$platformName ($domain) was filtered locally by BetShield DNS.",
        )
      }
    }
  }

  private fun registerNetworkRecoveryCallback() {
    val cm = getSystemService(ConnectivityManager::class.java) ?: return
    if (networkCallback != null) return
    val callback =
      object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          if (isRunning.get() && tunInterface == null) {
            if (establishTun()) {
              ServiceStatusBridge.setVpnState(VpnProtectionState.ACTIVE)
              launchPacketLoop()
            }
          }
        }
      }
    networkCallback = callback
    val request =
      NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    runCatching { cm.registerNetworkCallback(request, callback) }
  }

  private fun unregisterNetworkRecoveryCallback() {
    val callback = networkCallback ?: return
    networkCallback = null
    val cm = getSystemService(ConnectivityManager::class.java) ?: return
    runCatching { cm.unregisterNetworkCallback(callback) }
  }

  private fun stopProtection(paused: Boolean) {
    isRunning.set(false)
    packetLoopJob?.cancel()
    unregisterNetworkRecoveryCallback()
    runCatching { tunInterface?.close() }
    tunInterface = null
    ServiceStatusBridge.setVpnState(
      if (paused) VpnProtectionState.PAUSED else VpnProtectionState.STOPPED
    )
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  override fun onRevoke() {
    stopProtection(paused = false)
    super.onRevoke()
  }

  override fun onDestroy() {
    isRunning.set(false)
    packetLoopJob?.cancel()
    unregisterNetworkRecoveryCallback()
    runCatching { tunInterface?.close() }
    tunInterface = null
    serviceScope.cancel()
    if (ServiceStatusBridge.vpnState.value == VpnProtectionState.ACTIVE) {
      ServiceStatusBridge.setVpnState(VpnProtectionState.STOPPED)
    }
    super.onDestroy()
  }

  companion object {
    const val ACTION_START = "com.example.betshield.VPN_START"
    const val ACTION_STOP = "com.example.betshield.VPN_STOP"
    private const val VPN_CLIENT_IP = "10.120.0.1"
    private const val VPN_DNS_IP = "10.120.0.2"
    private val UPSTREAM_DNS_SERVERS = listOf("1.1.1.1", "8.8.8.8")
  }
}
