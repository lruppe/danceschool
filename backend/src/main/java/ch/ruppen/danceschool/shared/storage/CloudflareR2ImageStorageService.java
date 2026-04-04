package ch.ruppen.danceschool.shared.storage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.UUID;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
class CloudflareR2ImageStorageService implements ImageStorageService {

    private final ImageStorageProperties props;
    private S3Client s3Client;
    private String bucket;
    private String publicUrl;

    @PostConstruct
    void init() {
        requireNonBlank(props.r2Endpoint(), "app.image-storage.r2-endpoint");
        requireNonBlank(props.r2AccessKey(), "app.image-storage.r2-access-key");
        requireNonBlank(props.r2SecretKey(), "app.image-storage.r2-secret-key");
        requireNonBlank(props.r2Bucket(), "app.image-storage.r2-bucket");
        requireNonBlank(props.r2PublicUrl(), "app.image-storage.r2-public-url");

        this.bucket = props.r2Bucket();
        this.publicUrl = stripTrailingSlash(props.r2PublicUrl());
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(props.r2Endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.r2AccessKey(), props.r2SecretKey())))
                .region(Region.of("auto"))
                .forcePathStyle(true)
                .build();
        log.info("Cloudflare R2 image storage initialized for bucket {}", bucket);
    }

    @PreDestroy
    void shutdown() {
        if (s3Client != null) {
            s3Client.close();
            log.info("Cloudflare R2 S3Client closed");
        }
    }

    @Override
    public String store(byte[] data, String filename) {
        String extension = extractExtension(filename);
        String key = UUID.randomUUID() + extension;
        String contentType = resolveContentType(extension);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(data));

        return publicUrl + "/" + key;
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    private String resolveContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private static void requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required property '%s' is not set. All R2 properties must be configured for the prod profile."
                            .formatted(propertyName));
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
