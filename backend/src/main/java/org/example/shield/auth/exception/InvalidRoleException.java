package org.example.shield.auth.exception;

import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;

public class InvalidRoleException extends BusinessException {

    public InvalidRoleException() {
        super(ErrorCode.INVALID_ROLE);
    }
}
