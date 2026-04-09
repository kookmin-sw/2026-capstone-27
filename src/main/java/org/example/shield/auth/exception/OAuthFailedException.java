package org.example.shield.auth.exception;

import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;

public class OAuthFailedException extends BusinessException {

    public OAuthFailedException() {
        super(ErrorCode.OAUTH_FAILED);
    }
}
