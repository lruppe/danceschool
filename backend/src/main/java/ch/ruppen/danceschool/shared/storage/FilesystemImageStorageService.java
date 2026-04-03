package ch.ruppen.danceschool.shared.storage;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
class FilesystemImageStorageService implements ImageStorageService {

    private final Path storageDir;
    private final String baseUrl;

    FilesystemImageStorageService(String directory, String baseUrl) {
        this.storageDir = Path.of(directory);
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
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
}
