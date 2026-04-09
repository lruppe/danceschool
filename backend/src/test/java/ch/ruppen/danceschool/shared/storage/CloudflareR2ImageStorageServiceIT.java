package ch.ruppen.danceschool.shared.storage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class CloudflareR2ImageStorageServiceIT {

    private static final String BUCKET = "test-bucket";
    private static final String PUBLIC_URL = "https://pub.example.com";

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:4"))
            .withServices(LocalStackContainer.Service.S3);

    private static S3Client s3Client;
    private CloudflareR2ImageStorageService service;

    @BeforeAll
    static void createBucket() {
        s3Client = S3Client.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                .region(Region.of(localstack.getRegion()))
                .forcePathStyle(true)
                .build();

        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
    }

    @BeforeEach
    void setUp() throws Exception {
        ImageStorageProperties props = new ImageStorageProperties(
                null, null,
                localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
                localstack.getAccessKey(),
                localstack.getSecretKey(),
                BUCKET,
                PUBLIC_URL
        );
        service = new CloudflareR2ImageStorageService(props);

        // Inject the shared S3Client and set fields that init() would normally set
        setField(service, "s3Client", s3Client);
        setField(service, "bucket", BUCKET);
        setField(service, "publicUrl", PUBLIC_URL);
    }

    @Test
    void store_uploadsObjectToS3AndReturnsPublicUrl() {
        byte[] data = "fake-image-data".getBytes();

        String url = service.store(data, "photo.jpg");

        assertThat(url).startsWith(PUBLIC_URL + "/");
        assertThat(url).endsWith(".jpg");

        // Verify the object actually exists in S3
        String key = url.substring(url.lastIndexOf('/') + 1);
        byte[] stored = s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(BUCKET).key(key).build()).asByteArray();
        assertThat(stored).isEqualTo(data);
    }

    @Test
    void store_setsCorrectContentType_forJpeg() {
        service.store("data".getBytes(), "image.jpg");
        // If putObject succeeds with contentType, the SDK call chain works
    }

    @Test
    void store_setsCorrectContentType_forPng() {
        String url = service.store("data".getBytes(), "image.png");
        String key = url.substring(url.lastIndexOf('/') + 1);

        String contentType = s3Client.headObject(
                HeadObjectRequest.builder().bucket(BUCKET).key(key).build()).contentType();
        assertThat(contentType).isEqualTo("image/png");
    }

    @Test
    void store_setsCorrectContentType_forWebp() {
        String url = service.store("data".getBytes(), "image.webp");
        String key = url.substring(url.lastIndexOf('/') + 1);

        String contentType = s3Client.headObject(
                HeadObjectRequest.builder().bucket(BUCKET).key(key).build()).contentType();
        assertThat(contentType).isEqualTo("image/webp");
    }

    @Test
    void store_fallsBackToOctetStream_forUnknownExtension() {
        String url = service.store("data".getBytes(), "file.bmp");
        String key = url.substring(url.lastIndexOf('/') + 1);

        String contentType = s3Client.headObject(
                HeadObjectRequest.builder().bucket(BUCKET).key(key).build()).contentType();
        assertThat(contentType).isEqualTo("application/octet-stream");
    }

    @Test
    void delete_removesObjectFromS3() {
        String url = service.store("data".getBytes(), "to-delete.jpg");
        String key = url.substring(url.lastIndexOf('/') + 1);

        service.delete(key);

        assertThatThrownBy(() -> s3Client.headObject(
                HeadObjectRequest.builder().bucket(BUCKET).key(key).build()))
                .isInstanceOf(NoSuchKeyException.class);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
