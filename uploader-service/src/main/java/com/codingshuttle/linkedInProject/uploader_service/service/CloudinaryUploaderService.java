package com.codingshuttle.linkedInProject.uploader_service.service;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "uploader.backend", havingValue = "cloudinary", matchIfMissing = true)
public class CloudinaryUploaderService implements UploaderService{

    private final Cloudinary cloudinary;

    @Override
    public String upload(MultipartFile file) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), Map.of());
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void delete(String url) {
        String publicId = publicIdFromUrl(url);
        if (publicId == null) {
            log.warn("could not derive a Cloudinary public id from url {}, skipping delete", url);
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, Map.of());
            log.info("deleted Cloudinary asset {}", publicId);
        } catch (IOException e) {
            // Compensation is best-effort; surface as unchecked so the caller
            // can decide, but never let it corrupt the delete flow silently.
            throw new RuntimeException("Cloudinary delete failed for " + publicId, e);
        }
    }

    /**
     * Cloudinary secure_url looks like
     *   https://res.cloudinary.com/&lt;cloud&gt;/image/upload/v123456/&lt;publicId&gt;.&lt;ext&gt;
     * The public id is everything after the version segment, minus the extension.
     */
    private String publicIdFromUrl(String url) {
        if (url == null) return null;
        int uploadIdx = url.indexOf("/upload/");
        if (uploadIdx < 0) return null;

        String tail = url.substring(uploadIdx + "/upload/".length());
        tail = tail.replaceFirst("^v\\d+/", "");           // strip the version segment
        int dot = tail.lastIndexOf('.');
        return dot > 0 ? tail.substring(0, dot) : tail;    // strip the extension
    }
}
