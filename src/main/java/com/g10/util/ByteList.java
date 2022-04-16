package com.g10.util;

import javax.annotation.CheckForNull;
import java.util.Arrays;

public class ByteList {
    private final byte[] array;

    public ByteList(byte[] array) {
        this.array = array;
    }

    public int size() {
        return array.length;
    }

    public Byte get(int index) {
        return array[index];
    }

    public byte[] getArray() {
        return array;
    }

    @Override
    public boolean equals(@CheckForNull Object object) {
        if (object == this) {
            return true;
        } else if (object instanceof ByteList) {
            ByteList that = (ByteList) object;
            return Arrays.equals(this.array, that.array);
        } else {
            return false;
        }
    }

    /**
     * Adapted from com.google.common.primitives.Bytes.ByteArrayAsList#hashCode()
     */
    @Override
    public int hashCode() {
        int result = 1;
        for (byte b : array) {
            result = 31 * result + b;
        }
        return result;
    }
}
