package com.g10.cpen431.a7;

import java.io.IOException;
import java.net.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ca.NetSysLab.ProtocolBuffers.epidemic;
import com.g10.util.HashCircle;
import com.g10.util.NodeInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class TimeStampCommunication {
    private static final Logger logger = LogManager.getLogger(TimeStampCommunication.class);
    private static final int MAX_PACKET_SIZE = 20 * 8 + 100; /* 20 Servers * 8 Bytes/long + 100 Bytes just in case */
    private static final int numOfNodesToSend = 3;

    private final DatagramSocket socket;
    private final List<InetSocketAddress> nodesList;
    private final List<InetSocketAddress> communicationList;
    private final HashCircle hashCircle;

    private final int myNodeID;

    public TimeStampCommunication() throws SocketException {
        this.nodesList = NodeInfo.getServerList();
        this.communicationList = NodeInfo.getEpidemicProtocolList();
        this.myNodeID = nodesList.indexOf(NodeInfo.getLocalNodeInfo());
        this.hashCircle = HashCircle.getInstance();

        int port = communicationList.get(this.myNodeID).getPort();
        this.socket = new DatagramSocket(port);
    }

    public void run() {
        Thread receive = new Thread(this::receive);
        Thread send = new Thread(this::send);
        receive.start();
        send.start();
    }

    public void receive() {
        while (true) {
            byte[] receiveBuffer = new byte[MAX_PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(receiveBuffer, MAX_PACKET_SIZE);

            while (!socket.isClosed()) {
                try {
                    socket.receive(packet);
                    processPacket(packet);
                    packet.setData(receiveBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            logger.trace("Epidemic Protocol: Receive socket is closed.");
        }
    }

    public void send() {
        while (true) {
            int numOfAliveNodesSelected = 0;
            Random rand = new Random();
            int upperbound = nodesList.size();
            ArrayList<Integer> indexSelected = new ArrayList<>();
            int numOfAliveNodes= 0;
            for (int i = 0; i < upperbound; i++) {
                if (hashCircle.isAlive(i)) {
                    numOfAliveNodes++;
                }
            }
            if (numOfAliveNodes <= 1) {
                logger.info("Send thread: Only {} nodes alive", numOfAliveNodes);
                return;
            }

            while (numOfAliveNodesSelected < Math.min(numOfAliveNodes - 1, numOfNodesToSend)) {

                int index = rand.nextInt(upperbound);
                if (index != this.myNodeID) {
                    numOfAliveNodesSelected++;
                    indexSelected.add(index);
                }
            }

            ArrayList<Long> timeStampVector = hashCircle.getLocalTimestampVector();
            timeStampVector.set(this.myNodeID, System.currentTimeMillis());

            byte[] message = epidemic.Time_tag.newBuilder().addAllTimeTag(timeStampVector).build().toByteArray();
            DatagramPacket packet = new DatagramPacket(message, message.length);

            for (int i = 0; i < numOfNodesToSend; i++) {
                packet.setSocketAddress(communicationList.get(indexSelected.get(i)));
                try {
                    this.socket.send(packet);
                } catch(IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }

                logger.trace("Packet sent. {}", packet);
            }
            try {
                Thread.sleep((long) (HashCircle.T * 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
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
}
