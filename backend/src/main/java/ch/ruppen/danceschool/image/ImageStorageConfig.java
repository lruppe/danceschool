package ch.ruppen.danceschool.image;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(ImageStorageProperties.class)
class ImageStorageConfig {

    @Bean
    ImageStorageService imageStorageService(ImageStorageProperties props) {
        String provider = props.provider() != null ? props.provider() : "filesystem";
        return switch (provider) {
            case "r2" -> new CloudflareR2ImageStorageService(
                    props.r2Endpoint(),
                    props.r2AccessKey(),
                    props.r2SecretKey(),
                    props.r2Bucket(),
                    props.r2PublicUrl());
            default -> new FilesystemImageStorageService(
                    props.directory() != null ? props.directory() : "uploads",
                    props.baseUrl() != null ? props.baseUrl() : "http://localhost:8080/uploads");
        };
    }

    @Bean
    WebMvcConfigurer imageResourceConfigurer(ImageStorageProperties props) {
        String provider = props.provider() != null ? props.provider() : "filesystem";
        String directory = props.directory() != null ? props.directory() : "uploads";

        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                if ("filesystem".equals(provider)) {
                    registry.addResourceHandler("/uploads/**")
                            .addResourceLocations("file:" + directory + "/");
                }
            }
        };
    }
}
