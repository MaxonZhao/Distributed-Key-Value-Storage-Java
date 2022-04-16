package com.g10.cpen431.a12.membership;

import com.g10.cpen431.a12.NodeInfo;
import com.g10.cpen431.a12.Server;
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

    @Test
    void loadBalanceTest() throws NoSuchMethodException {
        Method logRingAnalysis = hc.getClass().getDeclaredMethod("logRingAnalysis");
        logRingAnalysis.setAccessible(true);
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
        System.out.println();

        NavigableMap<Integer, String> potentialNodesTailMap = tmap.tailMap(23, false);
        printTreeMap(potentialNodesTailMap);
        System.out.println();
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
