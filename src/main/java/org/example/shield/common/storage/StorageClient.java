package org.example.shield.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageClient {
    String upload(String path, MultipartFile file);
    void delete(String path);
    String getSignedUrl(String path, int expiresInSeconds);
}
