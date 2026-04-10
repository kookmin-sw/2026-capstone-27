package org.example.shield.auth.exception;

import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;

public class InvalidTokenException extends BusinessException {

    public InvalidTokenException() {
        super(ErrorCode.TOKEN_INVALID);
    }

    public InvalidTokenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
