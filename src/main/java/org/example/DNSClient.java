package org.example;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class DNSClient {
    private static final int QUERY_TYPE_A = 1;
    private static final int QUERY_TYPE_NS = 2;
    private static final int QUERY_CLASS_IN = 1;
    private static final String ROOT_DNS_SERVER = "192.5.5.241";

    public static void main(String[] args) {
        String hostname = "dns.google.com"; // Domain to resolve
        hostNameResolver(hostname);
    }

    private static void hostNameResolver(String hostname) {
        String currentServer = ROOT_DNS_SERVER;

        while (true) {
            System.out.println("Querying " + currentServer + " for " + hostname);
            byte[] dnsMessage = dnsQueryBuilder(hostname);
            byte[] response =DNSQuerySender(dnsMessage, currentServer);

            if (response != null) {
                int result = parseDNSResponse(response);
                if (result == -1) {
                    break;
                }

                // If the result is 2, we need to find the next server to query
                currentServer = resolveNSAndGetNextServer(response);
                if (currentServer == null) {
                    break; // Exit if no further server to query
                }
            }
        }
    }

    public static byte[] dnsQueryBuilder(String hostname) {
        ByteBuffer buffer = ByteBuffer.allocate(512);

        //this is head
        Random random = new Random();
        int id = random.nextInt(65536);
        short flags = 0x0000;
        short questionCount = 1;
        short answerCount = 0;
        short authorityCount = 0;
        short additionalCount = 0;

        // this is pack header
        buffer.putShort((short) id);
        buffer.putShort(flags);
        buffer.putShort(questionCount);
        buffer.putShort(answerCount);
        buffer.putShort(authorityCount);
        buffer.putShort(additionalCount);

        // this is question section
        byte[] encodedName = encodeHostname(hostname);
        buffer.put(encodedName);
        buffer.putShort((short) QUERY_TYPE_A);
        buffer.putShort((short) QUERY_CLASS_IN);

        return buffer.array();
    }

    private static byte[] encodeHostname(String hostname) {
        String[] labels = hostname.split("\\.");
        ByteBuffer buffer = ByteBuffer.allocate(256); // Allocate a buffer for the encoded name

        for (String label : labels) {
            byte[] labelBytes = label.getBytes(StandardCharsets.UTF_8);
            buffer.put((byte) labelBytes.length); // Length of the label
            buffer.put(labelBytes); // The label itself
        }
        buffer.put((byte) 0); // End of the hostname

        return buffer.array();
    }

    private static byte[] DNSQuerySender(byte[] dnsMessage, String server) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(server);
            DatagramPacket packet = new DatagramPacket(dnsMessage, dnsMessage.length, address, 53);

            socket.send(packet);

            byte[] responseBuffer = new byte[512];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.setSoTimeout(5000);

            socket.receive(responsePacket);
            return responsePacket.getData();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int parseDNSResponse(byte[] response) {
        ByteBuffer responseBuffer = ByteBuffer.wrap(response);

        // Parse header
        int id = responseBuffer.getShort(0) & 0xFFFF;
        short flags = responseBuffer.getShort(2);
        boolean isResponse = (flags & 0x8000) != 0; // QR bit check
        short questionCount = responseBuffer.getShort(4);
        short answerCount = responseBuffer.getShort(6);
        short authorityCount = responseBuffer.getShort(8);
        short additionalCount = responseBuffer.getShort(10);

        // Print response details
        System.out.println("Response ID: " + id);
        System.out.println("Is Response: " + isResponse);
        System.out.println("Questions: " + questionCount);
        System.out.println("Answers: " + answerCount);
        System.out.println("Authorities: " + authorityCount);
        System.out.println("Additionals: " + additionalCount);

        // Skip the question section
        int offset = 12 + getQuestionLength(responseBuffer, 12);

        // Parse answers
        for (int i = 0; i < answerCount; i++) {
            offset = parseAnswer(responseBuffer, offset);
        }

        // Parse authorities
        for (int i = 0; i < authorityCount; i++) {
            offset = parseAuthority(responseBuffer, offset);
        }

        return 0; // Indicate that parsing was successful
    }

    private static int getQuestionLength(ByteBuffer buffer, int offset) {
        int length = 0;
        while (true) {
            byte labelLength = buffer.get(offset++);
            length += 1;
            if (labelLength == 0) break;
            length += labelLength;
        }
        return length + 5;
    }

    private static int parseAnswer(ByteBuffer buffer, int offset) {
        // Read name (support for compression)
        int nameOffset = buffer.getShort(offset) & 0xFFFF;
        if ((nameOffset & 0xC000) == 0xC000) {
            nameOffset = nameOffset & 0x3FFF;
            offset += 2;
        } else {
            offset += getQuestionLength(buffer, offset);
        }


        short type = buffer.getShort(offset);
        short dnsClass = buffer.getShort(offset + 2);
        int ttl = buffer.getInt(offset + 4);
        short dataLength = buffer.getShort(offset + 8);

        offset += 10;

        if (type == QUERY_TYPE_A) {
            byte[] ipBytes = new byte[4];
            buffer.get(ipBytes, 0, 4);
            String ipAddress = String.format("%d.%d.%d.%d", ipBytes[0] & 0xFF, ipBytes[1] & 0xFF,
                    ipBytes[2] & 0xFF, ipBytes[3] & 0xFF);
            System.out.println("Answer: " + ipAddress + ", TTL: " + ttl);
            return -1; // Found the answer
        } else if (type == QUERY_TYPE_NS) {
            // Process NS record; return offset for further processing
            return offset + dataLength; // Skip over the NS record
        }

        return offset + dataLength; // Move past the data length
    }

    private static int parseAuthority(ByteBuffer buffer, int offset) {
        // Read name (support for compression)
        int nameOffset = buffer.getShort(offset) & 0xFFFF;
        if ((nameOffset & 0xC000) == 0xC000) {
            nameOffset = nameOffset & 0x3FFF;
            offset += 2;
        } else {
            offset += getQuestionLength(buffer, offset);
        }

        short type = buffer.getShort(offset);
        short dnsClass = buffer.getShort(offset + 2);
        int ttl = buffer.getInt(offset + 4);
        short dataLength = buffer.getShort(offset + 8);

        offset += 10;

        if (type == QUERY_TYPE_NS) {
            byte[] nsNameBytes = new byte[dataLength];
            buffer.get(nsNameBytes);
            String nsName = decodeHostname(nsNameBytes);
            System.out.println("Authoritative Name Server: " + nsName);
            return offset + dataLength; // Move past the NS record
        }

        return offset + dataLength; // Move past the data length
    }

    private static String decodeHostname(byte[] bytes) {
        StringBuilder hostname = new StringBuilder();
        int offset = 0;
        while (offset < bytes.length) {
            int length = bytes[offset++] & 0xFF;
            if (length == 0) break; // End of hostname
            if (hostname.length() > 0) hostname.append(".");
            hostname.append(new String(bytes, offset, length, StandardCharsets.UTF_8));
            offset += length;
        }
        return hostname.toString();
    }

    private static String resolveNSAndGetNextServer(byte[] response) {
        ByteBuffer responseBuffer = ByteBuffer.wrap(response);
        int offset = 12 + getQuestionLength(responseBuffer, 12);


        for (int i = 0; i < responseBuffer.getShort(6); i++) {
            offset = parseAnswer(responseBuffer, offset);
        }

        for (int i = 0; i < responseBuffer.getShort(8); i++) {
            offset = parseAuthority(responseBuffer, offset);
            if (offset >= responseBuffer.capacity()) {
                break;
            }
        }

        return null;
    }
}
