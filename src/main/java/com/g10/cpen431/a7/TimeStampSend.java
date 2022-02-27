package com.g10.cpen431.a7;

import ca.NetSysLab.ProtocolBuffers.Isalive;
import com.g10.util.HashCircle;
import com.g10.util.NodeInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.net.DatagramPacket;

public class TimeStampSend implements Closeable{

    private static final Logger logger = LogManager.getLogger(TimeStampSend.class);
    private final DatagramSocket serverSocket;
    private static final int numOfNodesToSend = 3;
    private static final int MAX_PACKET_SIZE = 17 * 1024; /* 16KB */
    private final int myNodeID;
    private final List<InetSocketAddress> nodesList;
    private final List<InetSocketAddress> communicationList;

    public TimeStampSend(InetSocketAddress myNodeAddress) throws IOException {
        this.nodesList = NodeInfo.getServerList();
        this.myNodeID = NodeInfo.getServerList().indexOf(myNodeAddress);
        this.communicationList = NodeInfo.getEpidemicProtocolList();
        this.serverSocket = new DatagramSocket(this.communicationList.get(this.myNodeID).getPort());
    }

    public void run() throws IOException{

        while (true) {
            int numOfAliveNodesSelected = 0;
            Random rand = new Random();
            int upperbound = nodesList.size();
            ArrayList<Integer> indexSelected = new ArrayList<Integer>();

            while (numOfAliveNodesSelected < numOfNodesToSend) {

                int index = rand.nextInt(upperbound);
                ArrayList<Boolean> status; //TODO: get the status array from hashCircle
                if (status.get[index] == 1 && nodesList.get(index) != NodeInfo.getLocalNodeInfo()) {

                    numOfAliveNodesSelected++;
                    indexSelected.add(index);
                }
            }

            ArrayList<Long> timeStampVector; //TODO: get the timeStamp vector from hashCircle
            timeStampVector.set(this.myNodeID, System.currentTimeMillis());

            byte[] message = Isalive.Is_alive.newBuilder().addAllTimeTag(timeStampVector).build().toByteArray();
            DatagramPacket packet = new DatagramPacket(message, MAX_PACKET_SIZE);

            for (int i = 0; i < numOfNodesToSend; i++) {
                packet.setSocketAddress(communicationList.get(indexSelected.get(i)));
                this.serverSocket.send(packet);
                logger.trace("Packet sent. {}", packet);
            }
        }

    }

    @Override
    public void close() throws IOException {
        logger.trace("Closing socket.");
        serverSocket.close();
    }
}
