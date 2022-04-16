package com.g10.cpen431.a11;

public class ServerException extends Exception {
    private final ErrCode errCode;

    public ServerException(ErrCode errCode) {
        this.errCode = errCode;
    }

    public ServerException(ErrCode errCode, String message) {
        super(message);
        this.errCode = errCode;
    }

    public ErrCode getErrCode() {
        return errCode;
    }
}
