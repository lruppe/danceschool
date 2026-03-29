package ch.ruppen.danceschool.image;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.image-storage")
public record ImageStorageProperties(
        /** "filesystem" or "r2" */
        String provider,

        /** Filesystem provider: directory to store images */
        String directory,

        /** Filesystem provider: base URL to serve images (e.g. http://localhost:8080/uploads) */
        String baseUrl,

        /** R2 provider: S3-compatible endpoint URL */
        String r2Endpoint,

        /** R2 provider: access key */
        String r2AccessKey,

        /** R2 provider: secret key */
        String r2SecretKey,

        /** R2 provider: bucket name */
        String r2Bucket,

        /** R2 provider: public URL prefix for the bucket */
        String r2PublicUrl
) {
}
