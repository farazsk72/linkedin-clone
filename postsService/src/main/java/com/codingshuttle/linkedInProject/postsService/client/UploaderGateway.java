package com.codingshuttle.linkedInProject.postsService.client;

import com.codingshuttle.linkedInProject.postsService.exception.BadRequestException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Resilient wrapper over the uploader Feign client. Unlike the connections
 * reads, an image upload cannot degrade to "empty" - there is no URL to
 * fabricate - so the fallback fails the request with a clear, retryable
 * message instead of silently dropping the user's image. The circuit still
 * protects post creation from hanging on a down uploader.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UploaderGateway {

    private final UploaderServiceClient client;

    @CircuitBreaker(name = "uploader", fallbackMethod = "uploadUnavailable")
    public String upload(MultipartFile file) {
        return client.uploadFile(file).getBody();
    }

    @SuppressWarnings("unused")
    private String uploadUnavailable(MultipartFile file, Throwable t) {
        log.warn("uploader unavailable, rejecting image upload: {}", t.toString());
        throw new BadRequestException("Image upload is temporarily unavailable - post without an image or try again shortly");
    }

    /**
     * Saga compensation - delete an uploaded image. Best-effort by design: a
     * compensation must never throw, or it would mask the original failure and
     * leave the saga half-rolled-back. If the delete cannot happen now it is
     * logged for later reconciliation.
     */
    @CircuitBreaker(name = "uploader", fallbackMethod = "deleteFailed")
    public void deleteQuietly(String url) {
        client.deleteFile(url);
        log.info("saga compensation: deleted uploaded image {}", url);
    }

    @SuppressWarnings("unused")
    private void deleteFailed(String url, Throwable t) {
        log.error("saga compensation could NOT delete image {} - needs manual cleanup: {}", url, t.toString());
    }
}
