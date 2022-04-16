package com.g10.cpen431.a12.membership;

import com.g10.cpen431.a12.NodeInfo;

import java.util.ArrayList;
import java.util.List;

public class VirtualNode {
    public final PhysicalNode physicalNode;
    public final boolean isLocal;
    public final int physicalIndex;
    public final long hashCode;
    public VirtualNode predecessor;

    private VirtualNode(PhysicalNode physicalNode, int physicalIndex, int virtualIndex, boolean isLocal) {
        String name = physicalNode + "VNODE#" + virtualIndex;
        this.physicalNode = physicalNode;
        this.hashCode = Hash.hash(name.getBytes());
        this.isLocal = isLocal;
        this.physicalIndex = physicalIndex;
    }

    /**
     * a method to generate virtual node based on a physical node. A physical node "possesses" a list of virtual nodes
     * a physical node can be uniquely identified with its ip address : port number.  i.e: 192.168.0.0.1: 5
     * a virtual node of this physical node in this implementation is represented as: 192.168.0.0.1VNODE#1:5
     * where the number after "#" denotes which virtual node of the corresponding physical node
     *
     * @param physicalIndex: a physical node (real node)
     */
    public static List<VirtualNode> generateVirtualNodes(int physicalIndex, int virtualCount) {
        PhysicalNode physicalNode = NodeInfo.getPhysicalNode(physicalIndex);
        boolean isLocal = physicalIndex == NodeInfo.getLocalIndex();
        ArrayList<VirtualNode> nodes = new ArrayList<>(virtualCount);
        for (int i = 0; i < virtualCount; i++) {
            nodes.add(new VirtualNode(physicalNode, physicalIndex, i, isLocal));
        }
        return nodes;
    }

    public boolean isAlive() {
        return MembershipService.isAlive(physicalIndex);
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public int hashCode() {
        return (int) hashCode;
    }
}
