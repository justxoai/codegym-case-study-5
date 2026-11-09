package vn.codegym.clinicbook.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String messageKey) {
        super(messageKey);
    }
}
