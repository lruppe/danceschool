package ch.ruppen.danceschool.school;

import java.util.List;

public record SchoolDetailDto(
        Long id,
        String name,
        String tagline,
        String about,
        String streetAddress,
        String city,
        String postalCode,
        String country,
        String phone,
        String email,
        String website,
        String coverImageUrl,
        String logoUrl,
        List<String> specialties,
        List<GalleryImageDto> galleryImages,
        List<YoutubeVideoDto> youtubeVideos
) {

    public record GalleryImageDto(String url, int position) {
    }

    public record YoutubeVideoDto(String url, int position) {
    }
}
