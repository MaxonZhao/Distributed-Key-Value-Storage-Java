package com.g10.cpen431.a7;

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

    public TimeStampSend() throws IOException {
        this.serverSocket = new DatagramSocket(port); //TODO: get port from Node info
    }

    public void run() throws IOException{

        List<InetSocketAddress> nodesList = NodeInfo.getServerList();

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
            timeStampVector.set(System.currentTimeMillis());

            DatagramPacket packet = new DatagramSocket(message, MAX_PACKET_SIZE); //TODO: new proto buffer message
            ArrayList<InetSocketAddress> communicationList = NodeInfo.getEpidemicList(); //TODO: communication addresses

            for (int i = 0; i < numOfNodesToSend; i++) {
                packet.setAddress(communicationList.get(indexSelected.get(i)).getAddress());
                packet.setPort(communicationList.get(indexSelected.get(i)).getPort());
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
