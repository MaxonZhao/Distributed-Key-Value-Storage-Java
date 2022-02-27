package com.g10.cpen431.a7;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import ca.NetSysLab.ProtocolBuffers.Message;
import ca.NetSysLab.ProtocolBuffers.epidemic;
import com.g10.util.HashCircle;
import com.g10.util.NodeInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.Logger;

import static java.lang.Math.max;

public class TimeStampReceive implements Closeable {
    private static final int MAX_PACKET_SIZE = 20 * 8 + 100; /* 20 Servers * 8 Bytes/long + 100 Bytes just in case */

    private final Logger logger;
    private final DatagramSocket socket;

    private final int myNodeID;

    public TimeStampReceive(Logger logger, InetSocketAddress myNodeID) throws SocketException {
        this.myNodeID = NodeInfo.getServerList().indexOf(myNodeID);
        int port = NodeInfo.getEpidemicProtocolList().get(this.myNodeID).getPort();
        logger.info("Port in use by the receive thread: {}", port);
        this.socket = new DatagramSocket(port);
        this.logger = logger;

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

        epidemic.Time_tag time_tag;
        try {
            time_tag = epidemic.Time_tag.PARSER.parseFrom(packet.getData(), 0, packet.getLength());
        } catch (InvalidProtocolBufferException e) {
            logger.warn("Unable to parse epidemic protocol packet. Packet: {}", packet);
            return;
        }

        mergeToLocal(time_tag.getTimeTagList());
    }

    private void mergeToLocal(List<Long> remote_timestamp_vector) {
        logger.trace("Epidemic Protocol: Received remote_timestamp_vector: {}.", remote_timestamp_vector);

        for (int i = 0; i < remote_timestamp_vector.size(); i++) {
            if (i == myNodeID)
                continue;
            
            List<Long> local_timestamp_vector = HashCircle.getInstance().getLocalTimestampVector();
            local_timestamp_vector.set(i, max(local_timestamp_vector.get(i), remote_timestamp_vector.get(i)));
        }
    }

    @Override
    public void close() throws IOException {

    }
}
