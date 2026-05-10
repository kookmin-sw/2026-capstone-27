package org.example.shield.brief.exception;

import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;

public class BriefAlreadyConfirmedException extends BusinessException {
    public BriefAlreadyConfirmedException() {
        super(ErrorCode.BRIEF_ALREADY_CONFIRMED);
    }
}
