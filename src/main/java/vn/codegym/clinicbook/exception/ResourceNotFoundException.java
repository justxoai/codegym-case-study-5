package vn.codegym.clinicbook.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String messageKey) {
        super(messageKey);
    }
}
