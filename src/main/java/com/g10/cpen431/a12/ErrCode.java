package com.g10.cpen431.a12;

public enum ErrCode {
    SUCCESS(0x00), UNKNOWN_KEY(0x01), OUT_OF_SPACE(0x02), OVERLOAD(0x03),
    INTERNAL_FAILURE(0x04), UNKNOWN_COMMAND(0x05), INVALID_KEY(0x06), INVALID_VALUE(0x07),
    MALFORMED_REQUEST(0x20);

    public final int val;

    ErrCode(int value) {
        this.val = value;
    }
}
