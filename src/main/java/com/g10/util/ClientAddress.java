package com.g10.util;

import com.matei.eece411.util.ByteOrder;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Log4j2
@AllArgsConstructor
public class ClientAddress {
    private final InetAddress ip1;
    private final int ip2;
    private final int port;

    public ClientAddress(InetAddress ip, int port) {
        this(ip, 0, port);
    }

    public ClientAddress(int ip, int port) {
        this(null, ip, port);
    }

    public DatagramPacket newPacket(byte[] data) {
        InetAddress ip = ip1;
        if (ip == null) {
            Assertion.check(ip2 != 0);
            try {
                ip = InetAddress.getByAddress(ByteUtil.int2leb(ip2));
            } catch (UnknownHostException e) {
                log.fatal(e::toString);
                System.exit(1);
            }
        }
        return new DatagramPacket(data, data.length, ip, port);
    }

    public int getIp() {
        int ip = ip2;
        if (ip == 0) {
            Assertion.check(ip1 != null);
            return ByteOrder.leb2int(ip1.getAddress(), 0);
        }
        return ip;
    }

    public int getPort() {
        return port;
    }
}
