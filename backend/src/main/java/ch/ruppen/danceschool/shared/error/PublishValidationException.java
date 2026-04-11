package ch.ruppen.danceschool.shared.error;

import java.util.Map;

public class PublishValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public PublishValidationException(Map<String, String> fieldErrors) {
        super("Course is not ready to publish");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
