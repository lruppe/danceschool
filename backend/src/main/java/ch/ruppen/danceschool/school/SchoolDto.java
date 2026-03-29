package ch.ruppen.danceschool.school;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SchoolDto(
        Long id,
        @NotBlank String name,
        String streetAddress,
        String city,
        String postalCode,
        String country,
        String phone,
        @Email String email
) {
}
