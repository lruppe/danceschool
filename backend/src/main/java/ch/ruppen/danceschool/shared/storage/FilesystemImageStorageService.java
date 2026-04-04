package ch.ruppen.danceschool.shared.storage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
@Profile("!prod")
@RequiredArgsConstructor
class FilesystemImageStorageService implements ImageStorageService {

    private final ImageStorageProperties props;
    private Path storageDir;
    private String baseUrl;

    @PostConstruct
    void init() {
        String directory = props.directory() != null ? props.directory()
                : System.getProperty("java.io.tmpdir") + "/danceschool-uploads";
        this.storageDir = Path.of(directory);
        this.baseUrl = stripTrailingSlash(
                props.baseUrl() != null ? props.baseUrl() : "http://localhost:8080/uploads");
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create image storage directory: " + directory, e);
        }
        log.info("Filesystem image storage initialized at {}", storageDir.toAbsolutePath());
    }

    @Override
    public String store(byte[] data, String filename) {
        String extension = extractExtension(filename);
        String storedName = UUID.randomUUID() + extension;
        Path target = validatePath(storedName);
        try {
            Files.write(target, data);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store image: " + storedName, e);
        }
        return baseUrl + "/" + storedName;
    }

    @Override
    public void delete(String key) {
        Path target = validatePath(key);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete image: " + key, e);
        }
    }

    private Path validatePath(String name) {
        Path target = storageDir.resolve(name).normalize();
        if (!target.startsWith(storageDir.normalize())) {
            throw new IllegalArgumentException("Invalid file path: " + name);
        }
        return target;
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
