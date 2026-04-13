package org.example.shield.lawyer.exception;

import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;

public class LawyerNotFoundException extends BusinessException {

    public LawyerNotFoundException() {
        super(ErrorCode.LAWYER_NOT_FOUND);
    }
}
