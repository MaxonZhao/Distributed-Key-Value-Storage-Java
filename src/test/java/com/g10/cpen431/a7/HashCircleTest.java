package com.g10.cpen431.a7;

import org.junit.jupiter.api.Test;

import java.util.*;

class HashCircleTest {
    @Test
    void hashCircleTest() {
//        NodeInfo.initializeNodesList("src/main/java/com/g10/util/serverList.yml", 1);
//
//        HashCircle hc = HashCircle.getInstance();
//        byte[] index = {5,8,2,6,3,9,5,8, 4};
//        System.out.println("-------------");
//        int size = index.length;
////        for(int i=0;i<size;i++){
//            InetSocketAddress server = hc.findNodeFromHash(index);
//            System.out.println("index="+index +" hash="+ Hash.hash(index) +" server=" + server.getHostName());
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
