package org.example.shield.brief.exception;

import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;

import java.util.UUID;

public class BriefNotFoundException extends BusinessException {
    public BriefNotFoundException(UUID briefId) {
        super(ErrorCode.BRIEF_NOT_FOUND);
    }
}
