package com.g10.cpen431.a7;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import ca.NetSysLab.ProtocolBuffers.Isalive;
import ca.NetSysLab.ProtocolBuffers.Message;
import com.g10.util.NodeInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.Logger;

import static java.lang.Math.max;

public class TimeStampReceive implements Closeable {
    private static final int MAX_PACKET_SIZE = 20 * 8 + 100; /* 20 Servers * 8 Bytes/long + 100 Bytes just in case */

    private final Logger logger;
    private final DatagramSocket socket;

    private final int myNodeID;

    public TimeStampReceive(int port, Logger logger, InetSocketAddress myNodeID) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.logger = logger;
        this.myNodeID = NodeInfo.getServerList().indexOf(myNodeID);

        logger.info("Epidemic Protocol: Bound to port {}.", port);
    }

    public void run() {
        byte[] receiveBuffer = new byte[MAX_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(receiveBuffer, MAX_PACKET_SIZE);

        while (!socket.isClosed()) {
            try {
                socket.receive(packet);
                processPacket(packet);
                packet.setData(receiveBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logger.trace("Epidemic Protocol: Receive socket is closed.");
    }

    private void processPacket(DatagramPacket packet) {
        logger.trace("Epidemic Protocol: Received packet: {}", packet);

        // TODO: Check UDP checksum maybe

        Isalive.Is_alive is_alive;
        try {
            is_alive = Isalive.Is_alive.PARSER.parseFrom(packet.getData(), 0, packet.getLength());
        } catch (InvalidProtocolBufferException e) {
            logger.warn("Unable to parse epidemic protocol packet. Packet: {}", packet);
            return;
        }

        mergeToLocal(is_alive.getTimeTagList());
    }

    private void mergeToLocal(List<Long> remote_timestamp_vector) {
        for (int i = 0; i < remote_timestamp_vector.size(); i++) {
            if (i == myNodeID)
                continue;

            //TODO: Get local timestamp vector (synchronized?)
            List<Long> local_timestamp_vector = new ArrayList<Long>();

            local_timestamp_vector.set(i, max(local_timestamp_vector.get(i), remote_timestamp_vector.get(i)));
        }
    }

    @Override
    public void close() throws IOException {

    }
}
