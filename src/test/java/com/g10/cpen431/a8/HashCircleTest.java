package com.g10.cpen431.a8;

import com.g10.util.HashCircle;
import com.g10.util.LoggerConfiguration;
import com.g10.util.NodeInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.*;

class HashCircleTest {
    HashCircle hc;
    private static Logger logger;

    @BeforeEach
    void setup() {
        try {
            NodeInfo.initializeNodesList("src/test/resources/testServerList.yml", 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.hc = HashCircle.getInstance();
        initializeLogger("hashCircleTest");
    }

    @Test
    void virtualNodeToPhysicalNodeTest() {

        System.out.printf("list of all nodes: %s\n", NodeInfo.getServerList().toString());
        System.out.printf("list of all nodes for epidemic protocols: %s\n", NodeInfo.getEpidemicProtocolList().toString());

        InetSocketAddress pNode = NodeInfo.getServerList().get(2);
        InetSocketAddress vNode = new InetSocketAddress(pNode.getHostName() + "VNODE#1", pNode.getPort());
        System.out.printf("physical node: %s\n", pNode.getAddress());
        System.out.printf("virtual node: %s\n", vNode);
        InetSocketAddress pNodeParsed = hc.findPNodeFromVNode(vNode);
        System.out.printf("parsed physical node: %s\n", pNodeParsed);

        assert pNodeParsed.equals(pNode) == true;
    }

    @Test
    void physicalNodeToPhysicalNodeTest() {

        System.out.printf("list of all nodes: %s\n", NodeInfo.getServerList().toString());
        System.out.printf("list of all nodes for epidemic protocols: %s\n", NodeInfo.getEpidemicProtocolList().toString());

        InetSocketAddress pNode = NodeInfo.getServerList().get(2);
        System.out.printf("physical node: %s\n", pNode.getAddress());

        InetSocketAddress pNodeParsed = hc.findPNodeFromVNode(pNode);
        assert pNodeParsed.equals(pNode);
    }

    @Test
    void loadBalanceTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method logRingAnalysis = hc.getClass().getDeclaredMethod("logRingAnalysis");
        logRingAnalysis.setAccessible(true);
//        double sd = (double) logRingAnalysis.invoke(hc);
    }


    @Test
    void treeMapTest() {
        TreeMap<Integer, String> tmap =
                new TreeMap<Integer, String>();
        tmap.put(1, "Data1");
        tmap.put(23, "Data2");
        tmap.put(70, "Data3");
        tmap.put(90, "Data6");
        tmap.put(4, "Data4");
        tmap.put(2, "Data5");

        /* Display content using Iterator*/
        printTreeMap(tmap);
        System.out.println();

        NavigableMap<Integer, String> potentialNodesTailMap = tmap.tailMap(30, true);
        NavigableMap<Integer, String> potentialNodesHeadMap = tmap.headMap(30, true);
        printTreeMap(potentialNodesTailMap);
        System.out.println();
        printTreeMap(potentialNodesHeadMap);

    }

    private void initializeLogger(String label) {
        LoggerConfiguration.initialize(String.format("logs/server-%s.log", label));
        logger = LogManager.getLogger(Server.class);
        logger.always().log("Logger initialized.");
    }

    void printTreeMap(NavigableMap<Integer, String> tmap) {
        Set set = tmap.entrySet();
        Iterator iterator = set.iterator();
        while(iterator.hasNext()) {
            Map.Entry mEntry = (Map.Entry)iterator.next();
            System.out.print("key is: "+ mEntry.getKey() + " & Value is: ");
            System.out.println(mEntry.getValue());
        }
    }


}
