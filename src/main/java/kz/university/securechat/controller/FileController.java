package kz.university.securechat.controller;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Value("${minio.public.url:}")
    private String minioPublicUrl;

    public FileController(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    private String replaceWithPublicUrl(String presignedUrl) {
        if (minioPublicUrl == null || minioPublicUrl.isBlank()) return presignedUrl;
        try {
            java.net.URL url = new java.net.URL(presignedUrl);
            String internal = url.getProtocol() + "://" + url.getHost()
                    + (url.getPort() != -1 ? ":" + url.getPort() : "");
            return presignedUrl.replace(internal, minioPublicUrl);
        } catch (Exception e) {
            return presignedUrl;
        }
    }

    @GetMapping("/upload-url")
    public ResponseEntity<?> getUploadUrl(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        try {
            String objectName = UUID.randomUUID().toString() + ".enc";
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(10, TimeUnit.MINUTES)
                            .build()
            );
            return ResponseEntity.ok(Map.of(
                    "uploadUrl", replaceWithPublicUrl(url),
                    "objectName", objectName
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/download-url")
    public ResponseEntity<?> getDownloadUrl(@RequestParam String objectName, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
            return ResponseEntity.ok(Map.of("downloadUrl", replaceWithPublicUrl(url)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}