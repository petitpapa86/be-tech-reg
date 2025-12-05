package com.bcbs239.regtech.core.domain.shared;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ErrorDetail {
    private final String code;
    private final String message;
    private  String messageKey;;
    private  List<FieldError> fieldErrors;
    private final ErrorType errorType;

    private ErrorDetail(String code, ErrorType errorType,String message, String messageKey) {
        this.code = code;
        this.message = message;
        this.messageKey = messageKey;
        this.errorType = errorType;
    }
    private ErrorDetail(String code, ErrorType errorType,String message, List<FieldError> fieldErrors) {
        this.code = code;
        this.message = message;
        this.errorType = errorType;
        this.fieldErrors = fieldErrors;
    }

    public static ErrorDetail of(String code, ErrorType errorType, String message, String messageKey) {
        return new ErrorDetail(code, errorType, message, messageKey);
    }

    public static ErrorDetail validationError(List<FieldError> fieldErrors) {
        return new ErrorDetail("VALIDATION_ERROR", ErrorType.VALIDATION_ERROR, "Validation failed", fieldErrors);
    }


    public boolean hasFieldErrors() {
        return fieldErrors != null && !fieldErrors.isEmpty();
    }
}

