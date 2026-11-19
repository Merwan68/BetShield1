package com.example.service

import java.nio.ByteBuffer

data class ParsedIpv4UdpDnsPacket(
  val ipHeaderLength: Int,
  val sourceIp: ByteArray,
  val destIp: ByteArray,
  val sourcePort: Int,
  val destPort: Int,
  val dnsPayload: ByteArray,
  val queriedDomain: String?,
  val queryType: Int,
)

/**
 * Pure-Kotlin IPv4 / UDP / RFC 1035 DNS packet inspector and response builder.
 * Handles only UDP port 53 DNS queries; never inspects TCP or HTTPS traffic.
 */
object DnsPacketParser {
  private const val IP_PROTOCOL_UDP = 17
  private const val DNS_PORT = 53
  private const val DNS_HEADER_LEN = 12

  fun parseIpv4UdpDns(packet: ByteArray, length: Int = packet.size): ParsedIpv4UdpDnsPacket? {
    if (length < 28) return null
    val version = (packet[0].toInt() ushr 4) and 0x0F
    if (version != 4) return null

    val ihl = (packet[0].toInt() and 0x0F) * 4
    if (ihl < 20 || length < ihl + 8) return null

    val protocol = packet[9].toInt() and 0xFF
    if (protocol != IP_PROTOCOL_UDP) return null

    val totalLength = ((packet[2].toInt() and 0xFF) shl 8) or (packet[3].toInt() and 0xFF)
    val effectiveLen = minOf(length, totalLength)
    if (effectiveLen < ihl + 8) return null

    val sourceIp = packet.copyOfRange(12, 16)
    val destIp = packet.copyOfRange(16, 20)

    val sourcePort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
    val destPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
    if (destPort != DNS_PORT) return null

    val udpLen = ((packet[ihl + 4].toInt() and 0xFF) shl 8) or (packet[ihl + 5].toInt() and 0xFF)
    val dnsStart = ihl + 8
    val dnsEnd = minOf(effectiveLen, ihl + udpLen)
    if (dnsEnd - dnsStart < DNS_HEADER_LEN) return null

    val dnsPayload = packet.copyOfRange(dnsStart, dnsEnd)
    val question = parseDnsQuestion(dnsPayload)

    return ParsedIpv4UdpDnsPacket(
      ipHeaderLength = ihl,
      sourceIp = sourceIp,
      destIp = destIp,
      sourcePort = sourcePort,
      destPort = destPort,
      dnsPayload = dnsPayload,
      queriedDomain = question?.first,
      queryType = question?.second ?: 0,
    )
  }

  fun parseDnsQuestion(dnsPayload: ByteArray): Pair<String, Int>? {
    if (dnsPayload.size < DNS_HEADER_LEN + 5) return null
    val qdCount = ((dnsPayload[4].toInt() and 0xFF) shl 8) or (dnsPayload[5].toInt() and 0xFF)
    if (qdCount < 1) return null

    val labels = mutableListOf<String>()
    var idx = DNS_HEADER_LEN
    var safety = 0
    while (idx < dnsPayload.size && safety < 64) {
      safety++
      val labelLen = dnsPayload[idx].toInt() and 0xFF
      if (labelLen == 0) {
        idx++
        break
      }
      // Compression pointer isn't expected in first QNAME, abort safely if encountered
      if ((labelLen and 0xC0) == 0xC0) return null
      idx++
      if (idx + labelLen > dnsPayload.size) return null
      val label = String(dnsPayload, idx, labelLen, Charsets.US_ASCII)
      labels.add(label)
      idx += labelLen
    }

    if (labels.isEmpty() || idx + 4 > dnsPayload.size) return null
    val qType = ((dnsPayload[idx].toInt() and 0xFF) shl 8) or (dnsPayload[idx + 1].toInt() and 0xFF)
    return labels.joinToString(".").lowercase() to qType
  }

  /**
   * Crafts a valid DNS NXDOMAIN (RCODE = 3) response payload preserving transaction ID and question.
   */
  fun buildNxDomainDnsPayload(queryPayload: ByteArray): ByteArray {
    val response = queryPayload.copyOf()
    if (response.size < DNS_HEADER_LEN) return response
    // QR = 1 (response), preserve Opcode & RD
    val flagsHigh = (response[2].toInt() and 0x79) or 0x80
    // RA = 1, RCODE = 3 (NXDOMAIN)
    val flagsLow = 0x83
    response[2] = flagsHigh.toByte()
    response[3] = flagsLow.toByte()
    // ANCOUNT = 0, NSCOUNT = 0, ARCOUNT = 0
    for (i in 6 until 12) {
      response[i] = 0
    }
    // Truncate any EDNS0 additional records after question section if present
    val qEnd = findQuestionEndOffset(response)
    return if (qEnd in DNS_HEADER_LEN..response.size) {
      response.copyOfRange(0, qEnd)
    } else {
      response
    }
  }

  private fun findQuestionEndOffset(dnsPayload: ByteArray): Int {
    if (dnsPayload.size < DNS_HEADER_LEN) return dnsPayload.size
    var idx = DNS_HEADER_LEN
    while (idx < dnsPayload.size) {
      val len = dnsPayload[idx].toInt() and 0xFF
      if (len == 0) {
        idx++
        break
      }
      if ((len and 0xC0) == 0xC0) {
        idx += 2
        break
      }
      idx += 1 + len
    }
    return minOf(idx + 4, dnsPayload.size)
  }

  /**
   * Wraps a DNS response payload into a complete IPv4 + UDP packet ready to write to the TUN fd.
   */
  fun buildIpv4UdpResponsePacket(
    request: ParsedIpv4UdpDnsPacket,
    dnsResponsePayload: ByteArray,
  ): ByteArray {
    val ipHeaderLen = 20
    val udpHeaderLen = 8
    val udpTotalLen = udpHeaderLen + dnsResponsePayload.size
    val totalLen = ipHeaderLen + udpTotalLen

    val buffer = ByteBuffer.allocate(totalLen)

    // IPv4 Header (20 bytes): swap source and destination IP
    buffer.put(0x45.toByte()) // Version=4, IHL=5
    buffer.put(0x00.toByte()) // TOS
    buffer.putShort(totalLen.toShort())
    buffer.putShort(0.toShort()) // Identification
    buffer.putShort(0x4000.toShort()) // Don't fragment
    buffer.put(64.toByte()) // TTL
    buffer.put(IP_PROTOCOL_UDP.toByte())
    buffer.putShort(0.toShort()) // Header checksum placeholder
    buffer.put(request.destIp) // Src IP = original Dest IP
    buffer.put(request.sourceIp) // Dst IP = original Src IP

    // UDP Header (8 bytes): swap source and destination port
    buffer.putShort(request.destPort.toShort())
    buffer.putShort(request.sourcePort.toShort())
    buffer.putShort(udpTotalLen.toShort())
    buffer.putShort(0.toShort()) // UDP checksum placeholder
    buffer.put(dnsResponsePayload)

    val out = buffer.array()
    val ipChecksum = computeChecksum(out, 0, ipHeaderLen)
    out[10] = ((ipChecksum ushr 8) and 0xFF).toByte()
    out[11] = (ipChecksum and 0xFF).toByte()

    val udpChecksum = computeUdpChecksum(
      srcIp = request.destIp,
      dstIp = request.sourceIp,
      udpSegment = out,
      udpOffset = ipHeaderLen,
      udpLength = udpTotalLen,
    )
    out[ipHeaderLen + 6] = ((udpChecksum ushr 8) and 0xFF).toByte()
    out[ipHeaderLen + 7] = (udpChecksum and 0xFF).toByte()

    return out
  }

  fun computeChecksum(data: ByteArray, offset: Int, length: Int): Int {
    var sum = 0
    var i = offset
    val end = offset + length
    while (i + 1 < end) {
      val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
      sum += word
      i += 2
    }
    if (i < end) {
      sum += (data[i].toInt() and 0xFF) shl 8
    }
    while ((sum ushr 16) != 0) {
      sum = (sum and 0xFFFF) + (sum ushr 16)
    }
    return sum.inv() and 0xFFFF
  }

  private fun computeUdpChecksum(
    srcIp: ByteArray,
    dstIp: ByteArray,
    udpSegment: ByteArray,
    udpOffset: Int,
    udpLength: Int,
  ): Int {
    var sum = 0
    for (i in 0 until 4 step 2) {
      sum += ((srcIp[i].toInt() and 0xFF) shl 8) or (srcIp[i + 1].toInt() and 0xFF)
      sum += ((dstIp[i].toInt() and 0xFF) shl 8) or (dstIp[i + 1].toInt() and 0xFF)
    }
    sum += IP_PROTOCOL_UDP
    sum += udpLength

    var i = udpOffset
    val end = udpOffset + udpLength
    while (i + 1 < end) {
      val word = ((udpSegment[i].toInt() and 0xFF) shl 8) or (udpSegment[i + 1].toInt() and 0xFF)
      sum += word
      i += 2
    }
    if (i < end) {
      sum += (udpSegment[i].toInt() and 0xFF) shl 8
    }
    while ((sum ushr 16) != 0) {
      sum = (sum and 0xFFFF) + (sum ushr 16)
    }
    val result = sum.inv() and 0xFFFF
    return if (result == 0) 0xFFFF else result
  }
}
