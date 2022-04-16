package com.g10.cpen431.a11.membership;

import com.g10.cpen431.a11.NodeInfo;
import com.g10.cpen431.a11.Server;
import com.g10.util.LoggerConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class HashCircleTest {
    HashCircle hc;

    @BeforeEach
    void setup() {
        try {
            NodeInfo.init("src/test/resources/testServerList.yml", 1);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        this.hc = HashCircle.getInstance();
        initializeLogger("hashCircleTest");
    }

//    @Test
//    void virtualNodeToPhysicalNodeTest() {
//
//        System.out.printf("list of all nodes: %s\n", NodeInfo.getServerList().toString());
//        System.out.printf("list of all nodes for epidemic protocols: %s\n", NodeInfo.getEpidemicProtocolList().toString());
//
//        InetSocketAddress pNode = NodeInfo.getServerList().get(2);
//        InetSocketAddress vNode = new InetSocketAddress(pNode.getHostName() + "VNODE#1", pNode.getPort());
//        System.out.printf("physical node: %s\n", pNode.getAddress());
//        System.out.printf("virtual node: %s\n", vNode);
//        InetSocketAddress pNodeParsed = hc.findPNodeFromVNode(vNode);
//        System.out.printf("parsed physical node: %s\n", pNodeParsed);
//
//
//        assertEquals(pNode, pNodeParsed);
//    }
//
//    @Test
//    void physicalNodeToPhysicalNodeTest() {
//
//        System.out.printf("list of all nodes: %s\n", NodeInfo.getServerList().toString());
//        System.out.printf("list of all nodes for epidemic protocols: %s\n", NodeInfo.getEpidemicProtocolList().toString());
//
//        InetSocketAddress pNode = NodeInfo.getServerList().get(2);
//        System.out.printf("physical node: %s\n", pNode.getAddress());
//
//        InetSocketAddress pNodeParsed = hc.findPNodeFromVNode(pNode);
//        assertEquals(pNode, pNodeParsed);
//    }

    @Test
    void loadBalanceTest() throws NoSuchMethodException {
        Method logRingAnalysis = hc.getClass().getDeclaredMethod("logRingAnalysis");
        logRingAnalysis.setAccessible(true);
//        double sd = (double) logRingAnalysis.invoke(hc);
    }


    @Test
    void treeMapTest() {
        TreeMap<Integer, String> tmap =
                new TreeMap<>();
        tmap.put(1, "Data1");
        tmap.put(23, "Data2");
        tmap.put(70, "Data3");
        tmap.put(90, "Data6");
        tmap.put(4, "Data4");
        tmap.put(2, "Data5");

        /* Display content using Iterator*/
//        printTreeMap(tmap);
        System.out.println();

        NavigableMap<Integer, String> potentialNodesTailMap = tmap.tailMap(23, false);
//        NavigableMap<Integer, String> potentialNodesHeadMap = tmap.headMap(70, true);
        printTreeMap(potentialNodesTailMap);
        System.out.println();
//        printTreeMap(potentialNodesHeadMap);

    }

    private void initializeLogger(String label) {
        LoggerConfiguration.initialize(String.format("logs/server-%s.log", label));
        Logger logger = LogManager.getLogger(Server.class);
        logger.always().log("Logger initialized.");
    }

    void printTreeMap(NavigableMap<Integer, String> tmap) {
        Set<Map.Entry<Integer, String>> set = tmap.entrySet();
        for (Map.Entry<Integer, String> integerStringEntry : set) {
            System.out.print("key is: " + integerStringEntry.getKey() + " & Value is: ");
            System.out.println(integerStringEntry.getValue());
        }
    }

}
