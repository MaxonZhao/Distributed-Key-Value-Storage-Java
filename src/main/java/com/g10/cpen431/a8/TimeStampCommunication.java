package com.g10.cpen431.a8;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ca.NetSysLab.ProtocolBuffers.Epidemic;
import com.g10.util.NodeInfo;
import com.g10.util.NodesManager;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TimeStampCommunication {
    private static final Logger logger = LogManager.getLogger(TimeStampCommunication.class);
    private static final int MAX_PACKET_SIZE = 100 * 8 + 100; /* 100 Servers * 8 Bytes/long + 100 Bytes just in case */
    private static final int numOfNodesToSend = 3;

    private final DatagramSocket socket;
    private final List<InetSocketAddress> communicationList;
    private final int myNodeID;

    public TimeStampCommunication() throws SocketException {
        this.communicationList = NodeInfo.getEpidemicProtocolList();
        this.myNodeID = NodeInfo.getLocalIndex();
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
            Random rand = new Random();

            int numberOfNodesToSend = Math.min(communicationList.size() - 1, numOfNodesToSend);
            List<InetSocketAddress> selectedAddress = new ArrayList<>(numberOfNodesToSend);
            for (int i = 0; i < numberOfNodesToSend; i++) {
                int targetNode;
                do {
                    targetNode = rand.nextInt(communicationList.size());
                } while (targetNode == this.myNodeID);
                selectedAddress.add(communicationList.get(targetNode));
            }

            List<Long> timeStampVector = NodesManager.getLocalTimestampVector();
            byte[] message = Epidemic.EpidemicInfo.newBuilder().addAllTimeTag(timeStampVector).build().toByteArray();
            DatagramPacket packet = new DatagramPacket(message, message.length);

//            logger.info("Sending vector: {}", timeStampVector);
            logger.info("Sending to {} nodes: {}", selectedAddress.size(), selectedAddress);
            for (InetSocketAddress address : selectedAddress) {
                packet.setSocketAddress(address);
                try {
                    this.socket.send(packet);
                } catch (IOException e) {
                    logger.error("IOException - {} {}", e::getMessage, e::getStackTrace);
                }
            }
            try {
                Thread.sleep((long) (NodesManager.T * 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private void processPacket(DatagramPacket packet) {
        logger.trace("Epidemic Protocol: Received packet: {}", packet);

        // TODO: Check UDP checksum maybe

        Epidemic.EpidemicInfo timeTag;
        try {
            timeTag = Epidemic.EpidemicInfo.PARSER.parseFrom(packet.getData(), 0, packet.getLength());
        } catch (InvalidProtocolBufferException e) {
            logger.error("Unable to parse epidemic protocol packet. Packet: {}", packet);
            return;
        }

        NodesManager.mergeLocalTimestampVector(timeTag.getTimeTagList());
    }
}
