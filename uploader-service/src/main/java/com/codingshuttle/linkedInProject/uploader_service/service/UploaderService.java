package com.codingshuttle.linkedInProject.uploader_service.service;

import org.springframework.web.multipart.MultipartFile;

public interface UploaderService {

    String upload(MultipartFile file);

    /** Best-effort delete of a previously uploaded file, by its returned URL. */
    void delete(String url);

}
