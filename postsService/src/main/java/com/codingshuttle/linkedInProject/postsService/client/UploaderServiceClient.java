package com.codingshuttle.linkedInProject.postsService.client;

import com.codingshuttle.linkedInProject.postsService.config.AppConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "uploader-service", path = "/uploads/file", url = "${UPLOADER_SERVICE_URI:http://localhost:9050}", configuration = AppConfig.class)
public interface UploaderServiceClient {

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file);

    /** Saga compensation: delete a previously uploaded file by its URL. */
    @org.springframework.web.bind.annotation.DeleteMapping
    void deleteFile(@org.springframework.web.bind.annotation.RequestParam("url") String url);
}
