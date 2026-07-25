package com.codingshuttle.linkedInProject.uploader_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Serves locally-stored uploads back over HTTP when the local storage backend
 * is active. GET {@code /file/**} (i.e. {@code /uploads/file/**} once the
 * context-path is applied) maps onto files on disk, with the content-type
 * inferred from the extension. The POST/DELETE handlers on {@code /file} stay
 * with {@link com.codingshuttle.linkedInProject.uploader_service.UploaderController}
 * - controller mappings win, so this only catches the GET reads. Registered
 * only for {@code uploader.backend=local}.
 */
@Configuration
@ConditionalOnProperty(name = "uploader.backend", havingValue = "local")
public class LocalStorageWebConfig implements WebMvcConfigurer {

    private final String location;

    public LocalStorageWebConfig(@Value("${upload.local.dir:./uploads-data}") String dir) {
        // addResourceLocations needs a file: URL ending in a slash.
        String uri = Paths.get(dir).toAbsolutePath().normalize().toUri().toString();
        this.location = uri.endsWith("/") ? uri : uri + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/file/**")
                .addResourceLocations(location);
    }
}
