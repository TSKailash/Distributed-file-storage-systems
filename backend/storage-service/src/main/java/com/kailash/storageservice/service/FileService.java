package com.kailash.storageservice.service;

import com.kailash.storageservice.exception.FileNotFound;
import com.kailash.storageservice.exception.FileValidationException;
import com.kailash.storageservice.exception.StorageException;
import com.kailash.storageservice.model.FileMetaData;
import com.kailash.storageservice.repository.FileMetadataRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import com.kailash.storageservice.dto.FileDownload;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger logger =
            LoggerFactory.getLogger(FileService.class);

    @Value("${minio.bucket-name}")
    private String bucketName;

    private final FileMetadataRepository fileMetadataRepository;

    private final RedisTemplate<String, FileMetaData> redisTemplate;

    private final MinioClient minioClient;

    public FileService(
            FileMetadataRepository fileMetadataRepository,
            RedisTemplate<String, FileMetaData> redisTemplate,
            MinioClient minioClient
    ) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.redisTemplate = redisTemplate;
        this.minioClient = minioClient;
    }

    private String getCacheKey(UUID id) {
        return "file:" + id;
    }

    private FileMetaData getFromCache(UUID id) {

        String key = getCacheKey(id);

        FileMetaData data =
                redisTemplate.opsForValue().get(key);

        if (data != null) {
            logger.info(
                    "Redis cache HIT for fileId={}",
                    id
            );
            return data;
        }

        logger.info(
                "Redis cache MISS for fileId={}",
                id
        );

        return null;
    }

    private void saveToCache(FileMetaData data) {

        String key = getCacheKey(data.getId());

        redisTemplate
                .opsForValue()
                .set(
                        key,
                        data,
                        Duration.ofMinutes(10)
                );

        logger.info(
                "Saved fileId={} to Redis cache",
                data.getId()
        );
    }

    private void rollbackFile(Path filePath) {

        try {

            Files.deleteIfExists(filePath);

        } catch (IOException e) {
            logger.error(
                    "Rollback failed. Could not delete {}",
                    filePath,
                    e
            );
            // Logging will be added later
        }
    }



    private void deleteFromCache(UUID id) {

        String key = getCacheKey(id);

        redisTemplate.delete(key);

        logger.info(
                "Cache invalidated for fileId={}",
                id
        );
    }

    private void uploadObject(
            MultipartFile file,
            String objectName
    ) {

        try {

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(
                                    file.getInputStream(),
                                    file.getSize(),
                                    -1
                            )
                            .contentType(file.getContentType())
                            .build()
            );

        } catch (Exception e) {

            throw new StorageException(
                    "Failed to upload file to MinIO",
                    e
            );
        }
    }

    private void deleteObject(String objectName) {

        try {

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            logger.info(
                    "Deleted object from MinIO: {}",
                    objectName
            );

        } catch (Exception e) {

            logger.error(
                    "Failed to delete object {} from MinIO",
                    objectName,
                    e
            );
        }
    }

    public UUID uploadFile(MultipartFile file) {

        validateFile(file);

        logger.info(
                "Upload started. originalName={}, size={}, type={}",
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType()
        );

        String originalFileName = file.getOriginalFilename();

        String storedFileName = generateFileName(originalFileName);

        try {

            // Upload actual file to MinIO
            uploadObject(file, storedFileName);

            // Save metadata to PostgreSQL
            FileMetaData metadata = saveMetaData(
                    file,
                    originalFileName,
                    storedFileName
            );

            logger.info(
                    "Upload successful. fileId={}, objectName={}",
                    metadata.getId(),
                    storedFileName
            );

            return metadata.getId();

        } catch (Exception e) {

            logger.error(
                    "Upload failed. Rolling back MinIO object={}",
                    storedFileName,
                    e
            );

            // Remove orphan file from MinIO
            deleteObject(storedFileName);

            throw new StorageException(
                    "Failed to upload file",
                    e
            );
        }
    }


    public FileDownload downloadFile(UUID id) {

        logger.info(
                "Download requested. fileId={}",
                id
        );

        FileMetaData metadata = getFromCache(id);

        if (metadata == null) {

            metadata = fileMetadataRepository
                    .findById(id)
                    .orElseThrow(() ->
                            new FileNotFound(
                                    "File not found with id " + id
                            )
                    );

            saveToCache(metadata);
        }

        try {

            InputStreamResource resource =
                    new InputStreamResource(
                            minioClient.getObject(
                                    GetObjectArgs.builder()
                                            .bucket(bucketName)
                                            .object(metadata.getStoredName())
                                            .build()
                            )
                    );

            logger.info(
                    "Download successful. fileId={}, objectName={}",
                    id,
                    metadata.getStoredName()
            );

            return new FileDownload(
                    resource,
                    metadata.getOriginalName(),
                    metadata.getContentType()
            );

        } catch (Exception e) {

            throw new StorageException(
                    "Failed to download file from MinIO",
                    e
            );
        }
    }

    public void deleteFile(UUID id) {

        logger.info(
                "Delete requested. fileId={}",
                id
        );

        FileMetaData metadata = fileMetadataRepository
                .findById(id)
                .orElseThrow(() ->
                        new FileNotFound(
                                "File not found with id " + id
                        )
                );

        try {

            // Remove actual file
            deleteObject(metadata.getStoredName());

            // Remove metadata
            fileMetadataRepository.deleteById(id);

            // Remove cache
            deleteFromCache(id);

            logger.info(
                    "Delete successful. fileId={}, object={}",
                    id,
                    metadata.getStoredName()
            );

        } catch (Exception e) {

            throw new StorageException(
                    "Failed to delete file",
                    e
            );
        }
    }

    private void validateFile(MultipartFile file) {

        final long MAX_FILE_SIZE = 100 * 1024 * 1024;

        if (file.isEmpty()) {
            throw new FileValidationException(
                    "File cannot be empty"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileValidationException(
                    "File size limit exceeded (Max 100 MB)"
            );
        }

        List<String> allowedTypes = List.of(
                "application/pdf",
                "image/jpeg",
                "image/png",
                "video/mp4"
        );

        if (!allowedTypes.contains(file.getContentType())) {
            throw new FileValidationException(
                    "Unsupported file type"
            );
        }
    }

    private String generateFileName(String originalFileName) {

        String extension = "";

        if (originalFileName != null &&
                originalFileName.contains(".")) {

            extension = originalFileName.substring(
                    originalFileName.lastIndexOf(".")
            );
        }

        return UUID.randomUUID() + extension;
    }

    private void storeFile(MultipartFile file, String fileName) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(
                                    file.getInputStream(),
                                    file.getSize(),
                                    -1
                            )
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {

            throw new StorageException(
                    "Failed to Upload to MinIO",
                    e
            );
        }
    }

    private FileMetaData saveMetaData(
            MultipartFile file,
            String originalFileName,
            String storedFileName
    ) {

        FileMetaData metadata = new FileMetaData(
                originalFileName,
                storedFileName,
                file.getSize(),
                file.getContentType()
        );

        return fileMetadataRepository.save(metadata);
    }
}