package ch.ruppen.danceschool.image;

import ch.ruppen.danceschool.shared.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
class ImageController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    private final ImageStorageService imageStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ImageUploadResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ImageUploadException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ImageUploadException("File exceeds the maximum allowed size of 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ImageUploadException("Only JPEG, PNG, and WebP images are accepted");
        }

        String url = imageStorageService.store(file.getBytes(), file.getOriginalFilename());
        return new ImageUploadResponse(url);
    }

    @ExceptionHandler(ImageUploadException.class)
    ProblemDetail handleImageUpload(ImageUploadException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Image Upload");
        return problem;
    }

    static class ImageUploadException extends RuntimeException {
        ImageUploadException(String message) {
            super(message);
        }
    }
}
