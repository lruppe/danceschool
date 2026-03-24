package ch.ruppen.danceschool.shared.error;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Object id) {
        super("%s with id %s not found".formatted(resourceName, id));
    }
}
