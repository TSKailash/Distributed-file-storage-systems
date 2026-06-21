package com.kailash.storageservice.service;

import com.kailash.storageservice.exception.FileNotFound;
import com.kailash.storageservice.exception.FileValidationException;
import com.kailash.storageservice.exception.StorageException;
import com.kailash.storageservice.model.FileMetaData;
import com.kailash.storageservice.repository.FileMetadataRepository;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {

    private final Logger logger=LoggerFactory.getLogger(FileService.class);

    private final Path storagePath = Paths.get("storage");

    private final FileMetadataRepository fileMetadataRepository;

    public FileService(FileMetadataRepository fileMetadataRepository) {
        this.fileMetadataRepository = fileMetadataRepository;

        try {
            Files.createDirectories(storagePath);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
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


    public UUID uploadFile(MultipartFile file) {
        validateFile(file);
        logger.info(
                "Upload started. originalName={}, size={} bytes, type={}",
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType()
        );

        String originalFileName = file.getOriginalFilename();

        String uniqueFileName=generateFileName(originalFileName);

        Path filePath=null;
        try {
            filePath=storeFile(file, uniqueFileName);
            FileMetaData metaData=saveMetaData(file, originalFileName, uniqueFileName);
            logger.info(
                    "Upload successful. fileId={}, storedName={}",
                    metaData.getId(),
                    metaData.getStoredName()
            );
            return metaData.getId();
        } catch (Exception ex) {
            if(filePath !=null){
                logger.error(
                        "Upload failed. Rolling back file {}",
                        uniqueFileName, ex
                );
                rollbackFile(filePath);
            }
            throw new StorageException(
                    "Upload failed. Rolled back stored file",
                    ex
            );
        }
    }


    public Resource downloadFile(UUID id) {
        try {
            logger.info(
                    "Download requested. fileId={}",
                    id
            );
            Optional<FileMetaData> data=fileMetadataRepository.findById(id);
            if(data.isEmpty()){
                throw new FileNotFound("File not found with id: "+id);
            }

            String filename=data.get().getStoredName();

            Path filePath = storagePath.resolve(filename).normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new FileNotFound("File not found: " + filename);
            }
            logger.info(
                    "Download successful. fileId={}, storedName={}",
                    id,
                    data.get().getStoredName()
            );

            return resource;

        } catch (MalformedURLException e) {
            throw new StorageException("Unable to access file", e);
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
                        new FileNotFound("File not found with id: " + id)
                );
        Path filePath = storagePath
                .resolve(metadata.getStoredName())
                .normalize();
        try {
            Files.delete(filePath);
            try {
                // Delete metadata after successful file deletion
                fileMetadataRepository.deleteById(id);
            } catch (Exception ex) {
                throw new StorageException(
                        "File deleted but metadata cleanup failed. System is inconsistent.",
                        ex
                );
            }
        } catch (IOException ex) {

            throw new StorageException(
                    "Failed to delete file from storage",
                    ex
            );
        }
        logger.info(
                "Delete successful. fileId={}, storedName={}",
                id,
                metadata.getStoredName()
        );
    }

    public void validateFile(MultipartFile file){
        final long MAX_FILE_SIZE = 100 * 1024 * 1024;

        if (file.isEmpty()) {
            throw new FileValidationException("File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileValidationException("File size limit exceeded!! (Must be 100MB or below)");
        }

        List<String> allowedTypes = List.of(
                "application/pdf",
                "image/jpeg",
                "image/png",
                "video/mp4"
        );
        if (!allowedTypes.contains(file.getContentType())) {
            throw new FileValidationException("Unsupported file type");
        }
    }

    public String generateFileName(String originalFileName){
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }

    private Path storeFile(MultipartFile file, String fileName) {

        Path filePath = storagePath
                .resolve(fileName)
                .normalize();

        try {

            Files.copy(
                    file.getInputStream(),
                    filePath
            );

            return filePath;

        } catch (IOException e) {

            throw new StorageException(
                    "Failed to store file",
                    e
            );
        }
    }

    public FileMetaData saveMetaData(MultipartFile file, String originalFileName, String storedFileName){
        FileMetaData metaData=new FileMetaData(
                originalFileName,
                storedFileName,
                file.getSize(),
                file.getContentType()
        );

        return fileMetadataRepository.save(metaData);
    }
}