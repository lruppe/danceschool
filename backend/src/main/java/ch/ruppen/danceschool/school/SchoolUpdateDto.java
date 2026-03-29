package ch.ruppen.danceschool.school;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

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
        String logoUrl
) {
}
