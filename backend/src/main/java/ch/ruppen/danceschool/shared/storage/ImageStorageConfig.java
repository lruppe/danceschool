package ch.ruppen.danceschool.shared.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(ImageStorageProperties.class)
class ImageStorageConfig {

    @Bean
    @Profile("!prod")
    WebMvcConfigurer imageResourceConfigurer(ImageStorageProperties props) {
        String directory = props.directory() != null ? props.directory()
                : System.getProperty("java.io.tmpdir") + "/danceschool-uploads";

        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations("file:" + directory + "/");
            }
        };
    }
}
