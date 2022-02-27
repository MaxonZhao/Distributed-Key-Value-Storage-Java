package com.g10.cpen431.a7;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import com.g10.util.NodeInfo;
import org.apache.logging.log4j.Logger;

import static java.lang.Math.max;

public class TimeStampReceive implements Closeable {
    private static final int MAX_PACKET_SIZE = 20 * 8 + 100; /* 20 Servers * 8 Bytes/long */

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

        //TODO: Parse the timestamp vector from packet
        List<Long> remote_timestamp_vector = new ArrayList<Long>();

        mergeToLocal(remote_timestamp_vector);
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
