package ch.ruppen.danceschool.image;

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
}
