package com.g10.cpen431.a7;

import ca.NetSysLab.ProtocolBuffers.epidemic;
import com.g10.util.HashCircle;
import com.g10.util.NodeInfo;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.net.DatagramPacket;
import java.util.concurrent.TimeUnit;

public class TimeStampSend implements Closeable{

    private final Logger logger;
    private final DatagramSocket serverSocket;
    private static final int numOfNodesToSend = 3;
    private static final int MAX_PACKET_SIZE = 17 * 1024; /* 16KB */
    private final int myNodeID;
    private final List<InetSocketAddress> nodesList;
    private final List<InetSocketAddress> communicationList;
    private final HashCircle hashCircle;

    public TimeStampSend(Logger logger, InetSocketAddress myNodeAddress) throws IOException {
        this.nodesList = NodeInfo.getServerList();
        this.myNodeID = NodeInfo.getServerList().indexOf(myNodeAddress);
        this.communicationList = NodeInfo.getEpidemicProtocolList();
        this.serverSocket = new DatagramSocket(this.communicationList.get(this.myNodeID).getPort());
        this.hashCircle = HashCircle.getInstance();
        this.logger = logger;
    }

    public void run(){

        while (true) {
            int numOfAliveNodesSelected = 0;
            Random rand = new Random();
            int upperbound = nodesList.size();
            ArrayList<Integer> indexSelected = new ArrayList<Integer>();

            while (numOfAliveNodesSelected < numOfNodesToSend) {

                int index = rand.nextInt(upperbound);
                if (hashCircle.isAlive(index) && index != this.myNodeID) {
                    numOfAliveNodesSelected++;
                    indexSelected.add(index);
                }
            }

            ArrayList<Long> timeStampVector = hashCircle.getLocalTimestampVector();
            timeStampVector.set(this.myNodeID, System.currentTimeMillis());

            byte[] message = epidemic.Time_tag.newBuilder().addAllTimeTag(timeStampVector).build().toByteArray();
            DatagramPacket packet = new DatagramPacket(message, MAX_PACKET_SIZE);

            for (int i = 0; i < numOfNodesToSend; i++) {
                packet.setSocketAddress(communicationList.get(indexSelected.get(i)));
                try{
                    this.serverSocket.send(packet);
                } catch(IOException e) {
                    e.printStackTrace();
                }

                logger.trace("Packet sent. {}", packet);
            }
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    logger.info("Send to {} nodes", numOfNodesToSend);
                }
            }, 0, (long) (HashCircle.T * 1000));
        }


    }

    @Override
    public void close() throws IOException {
        logger.trace("Closing socket.");
        serverSocket.close();
    }
}
