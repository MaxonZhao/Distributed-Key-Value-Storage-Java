package com.luuo.cpen431;

public enum Command {
    PUT(0x01), GET(0x02), REMOVE(0x03), SHUTDOWN(0x04),
    WIPE_OUT(0x05), IS_ALIVE(0x06), GET_PID(0x07), GET_MEMBERSHIP_COUNT(0x08);

    public final int val;

    Command(int value) {
        this.val = value;
    }

    public static Command get(int value) throws ServerException {
        switch (value) {
            case 0x01:
                return PUT;
            case 0x02:
                return GET;
            case 0x03:
                return REMOVE;
            case 0x04:
                return SHUTDOWN;
            case 0x05:
                return WIPE_OUT;
            case 0x06:
                return IS_ALIVE;
            case 0x07:
                return GET_PID;
            case 0x08:
                return GET_MEMBERSHIP_COUNT;
        }
        throw new ServerException(ErrCode.UNKNOWN_COMMAND);
    }
}
