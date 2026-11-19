package com.example

import com.example.service.DnsPacketParser
import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DnsPacketParserTest {

  @Test
  fun `parses IPv4 UDP DNS query and builds valid NXDOMAIN response`() {
    val dnsQuery = buildDnsQueryBytes("m.betika.et", txId = 0x1234)
    val ipUdpPacket = buildIpv4UdpPacket(dnsQuery)

    val parsed = DnsPacketParser.parseIpv4UdpDns(ipUdpPacket)
    assertNotNull(parsed)
    assertEquals("m.betika.et", parsed?.queriedDomain)
    assertEquals(1, parsed?.queryType) // Type A

    val nxPayload = DnsPacketParser.buildNxDomainDnsPayload(parsed!!.dnsPayload)
    // Verify TX ID preserved
    assertEquals(0x12.toByte(), nxPayload[0])
    assertEquals(0x34.toByte(), nxPayload[1])
    // Verify QR=1 and RCODE=3 (NXDOMAIN)
    assertEquals(1, (nxPayload[2].toInt() ushr 7) and 1)
    assertEquals(3, nxPayload[3].toInt() and 0x0F)

    val responseIpPacket = DnsPacketParser.buildIpv4UdpResponsePacket(parsed, nxPayload)
    // Verify source and destination IP swapped
    assertEquals(10, responseIpPacket[12].toInt() and 0xFF)
    assertEquals(120, responseIpPacket[13].toInt() and 0xFF)
    assertEquals(0, responseIpPacket[14].toInt() and 0xFF)
    assertEquals(2, responseIpPacket[15].toInt() and 0xFF)
    // Verify IPv4 header checksum validates to 0
    assertEquals(0, DnsPacketParser.computeChecksum(responseIpPacket, 0, 20))
  }

  private fun buildDnsQueryBytes(domain: String, txId: Int): ByteArray {
    val labels = domain.split(".")
    val qnameSize = labels.sumOf { 1 + it.length } + 1
    val buffer = ByteBuffer.allocate(12 + qnameSize + 4)
    buffer.putShort(txId.toShort())
    buffer.putShort(0x0100.toShort()) // Standard query, RD=1
    buffer.putShort(1.toShort()) // QDCOUNT=1
    buffer.putShort(0.toShort())
    buffer.putShort(0.toShort())
    buffer.putShort(0.toShort())
    for (label in labels) {
      buffer.put(label.length.toByte())
      buffer.put(label.toByteArray(Charsets.US_ASCII))
    }
    buffer.put(0.toByte())
    buffer.putShort(1.toShort()) // QTYPE=A
    buffer.putShort(1.toShort()) // QCLASS=IN
    return buffer.array()
  }

  private fun buildIpv4UdpPacket(payload: ByteArray): ByteArray {
    val udpLen = 8 + payload.size
    val totalLen = 20 + udpLen
    val buf = ByteBuffer.allocate(totalLen)
    buf.put(0x45.toByte())
    buf.put(0x00.toByte())
    buf.putShort(totalLen.toShort())
    buf.putShort(1.toShort())
    buf.putShort(0.toShort())
    buf.put(64.toByte())
    buf.put(17.toByte()) // UDP
    buf.putShort(0.toShort())
    buf.put(byteArrayOf(10, 120, 0, 1))
    buf.put(byteArrayOf(10, 120, 0, 2))
    buf.putShort(51234.toShort()) // src port
    buf.putShort(53.toShort()) // dst port
    buf.putShort(udpLen.toShort())
    buf.putShort(0.toShort())
    buf.put(payload)
    return buf.array()
  }
}
