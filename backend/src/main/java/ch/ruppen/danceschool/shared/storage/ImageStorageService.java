package ch.ruppen.danceschool.shared.storage;

/**
 * Abstraction for storing and deleting uploaded images.
 * Implementations select the backing store (local filesystem, Cloudflare R2, etc.).
 */
public interface ImageStorageService {

    /**
     * Store image data and return the public URL where it can be accessed.
     *
     * @param data     raw image bytes
     * @param filename original filename (used to derive the stored name)
     * @return public URL of the stored image
     */
    String store(byte[] data, String filename);

    /**
     * Delete a previously stored image.
     *
     * @param key storage key (filename or object key) identifying the image
     */
    void delete(String key);

    /**
     * Extract the storage key from a public URL.
     * Both storage implementations use a UUID-based filename as the last path segment.
     *
     * @param url public URL of the stored image
     * @return storage key, or null if the URL is null/empty
     */
    static String extractKey(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        int lastSlash = url.lastIndexOf('/');
        return lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
    }
}
