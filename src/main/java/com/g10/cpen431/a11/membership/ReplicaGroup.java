package com.g10.cpen431.a11.membership;

import java.util.ArrayList;

public class ReplicaGroup {
    public static final int N = 3;
    // the length of a replica group
    private static final HashCircle hc = HashCircle.getInstance();
    private final VirtualNode primary;
    private final ArrayList<VirtualNode> backups;

    // each replica group is uniquely identified by the hashcode of its primary virtual node
    public ReplicaGroup(VirtualNode primary) {
        this.primary = primary;
        this.backups = hc.findSuccessors(primary.hashCode, N);
    }

    public long getId() {
        return primary.hashCode;
    }

    public ArrayList<VirtualNode> getSuccessorList() {
        return backups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ReplicaGroup)) {
            return false;
        }
        ReplicaGroup that = (ReplicaGroup) o;
        if (!this.primary.equals(that.primary)) {
            return false;
        }
        return this.backups.equals(that.backups);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("G[").append(primary.physicalIndex).append('-');
        for (VirtualNode vn : backups) {
            sb.append(vn.physicalIndex).append(',');
        }
        sb.append(']');
        return sb.toString();
    }
}
