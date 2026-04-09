package ch.ruppen.danceschool.school;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.List;

public record SchoolUpdateDto(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String tagline,
        @Size(max = 4000) String about,
        @Size(max = 255) String streetAddress,
        @Size(max = 255) String city,
        @Size(max = 20) String postalCode,
        @Size(max = 255) String country,
        @Size(max = 50) String phone,
        @Email @Size(max = 255) String email,
        @URL @Size(max = 2000) String website,
        String coverImageUrl,
        String logoUrl,
        List<String> specialties,
        List<GalleryImageDto> galleryImages,
        List<YoutubeVideoDto> youtubeVideos
) {
    public record GalleryImageDto(String url, int position) {}
    public record YoutubeVideoDto(String url, int position) {}
}
