package ch.ruppen.danceschool.school;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import java.util.List;

public record SchoolUpdateDto(
        @NotBlank String name,
        String tagline,
        String about,
        String streetAddress,
        String city,
        String postalCode,
        String country,
        String phone,
        @Email String email,
        @URL String website,
        String coverImageUrl,
        String logoUrl,
        List<String> specialties,
        List<YoutubeVideoDto> youtubeVideos
) {
    public record YoutubeVideoDto(String url, int position) {}
}
