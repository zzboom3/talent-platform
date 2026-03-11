package com.talent.platform.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
public class OssService {

    @Value("${aliyun.oss.endpoint:https://oss-cn-chengdu.aliyuncs.com}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name:talent-platform-files}")
    private String bucketName;

    private OSS ossClient;

    @PostConstruct
    public void init() {
        if (accessKeyId != null && !accessKeyId.isBlank() && accessKeySecret != null && !accessKeySecret.isBlank()) {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            log.info("OSS client initialized for bucket: {}", bucketName);
        } else {
            log.warn("OSS credentials not configured, file upload will be disabled");
        }
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    public String uploadFile(MultipartFile file, String directory) throws IOException {
        if (ossClient == null) {
            throw new IllegalStateException("OSS is not configured. Please set OSS_ACCESS_KEY_ID and OSS_ACCESS_KEY_SECRET.");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectKey = directory + "/" + UUID.randomUUID() + ext;

        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(bucketName, objectKey, inputStream);
        }

        String endpointWithoutProtocol = endpoint.replaceFirst("^https?://", "");
        return String.format("https://%s.%s/%s", bucketName, endpointWithoutProtocol, objectKey);
    }

    public void deleteFile(String fileUrl) {
        if (ossClient == null) {
            throw new IllegalStateException("OSS is not configured. Please set OSS_ACCESS_KEY_ID and OSS_ACCESS_KEY_SECRET.");
        }
        String objectKey = extractObjectKeyFromUrl(fileUrl);
        if (objectKey != null) {
            ossClient.deleteObject(bucketName, objectKey);
        }
    }

    private String extractObjectKeyFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }
        String endpointWithoutProtocol = endpoint.replaceFirst("^https?://", "");
        String prefix = String.format("https://%s.%s/", bucketName, endpointWithoutProtocol);
        if (fileUrl.startsWith(prefix)) {
            return fileUrl.substring(prefix.length());
        }
        int slashIdx = fileUrl.indexOf("/", fileUrl.indexOf("//") + 2);
        if (slashIdx >= 0 && slashIdx < fileUrl.length() - 1) {
            return fileUrl.substring(slashIdx + 1);
        }
        return null;
    }
}
