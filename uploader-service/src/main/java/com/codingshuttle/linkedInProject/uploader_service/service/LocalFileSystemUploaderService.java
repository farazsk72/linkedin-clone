package com.codingshuttle.linkedInProject.uploader_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stores uploads on the local filesystem instead of a cloud bucket - handy for
 * local development where no real Cloudinary/GCS credentials are available.
 *
 * The returned URL points back at this service's GET serve endpoint
 * ({@code <public-base-url>/<name>}), which the gateway exposes unauthenticated
 * (GET only) so a plain {@code <img>} tag can load it. Active only when
 * {@code uploader.backend=local}.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "uploader.backend", havingValue = "local")
public class LocalFileSystemUploaderService implements UploaderService {

    private final Path storageDir;
    private final String publicBaseUrl;

    public LocalFileSystemUploaderService(
            @Value("${upload.local.dir:./uploads-data}") String dir,
            @Value("${upload.local.public-base-url:/api/v1/uploads/file}") String publicBaseUrl) {
        this.storageDir = Paths.get(dir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new UncheckedIOException("could not create local upload dir " + storageDir, e);
        }
        log.info("Local uploader active - storing files under {}", storageDir);
    }

    @Override
    public String upload(MultipartFile file) {
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        // Keep only the final path element, so a malicious name can't traverse.
        original = Paths.get(original).getFileName().toString();
        String name = UUID.randomUUID() + "-" + original;

        Path target = storageDir.resolve(name).normalize();
        if (!target.startsWith(storageDir)) {
            throw new IllegalArgumentException("resolved path escapes storage dir: " + name);
        }
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to store upload " + name, e);
        }
        return publicBaseUrl + "/" + name;
    }

    @Override
    public void delete(String url) {
        if (url == null) return;
        String name = url.substring(url.lastIndexOf('/') + 1);
        if (name.isBlank()) return;

        Path target = storageDir.resolve(name).normalize();
        if (!target.startsWith(storageDir)) {
            log.warn("refusing to delete outside storage dir: {}", url);
            return;
        }
        try {
            if (Files.deleteIfExists(target)) {
                log.info("deleted local upload {}", name);
            }
        } catch (IOException e) {
            // Compensation is best-effort; surface as unchecked, mirroring the
            // Cloudinary backend's contract.
            throw new UncheckedIOException("failed to delete upload " + name, e);
        }
    }
}
