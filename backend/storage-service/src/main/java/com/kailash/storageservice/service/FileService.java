package com.kailash.storageservice.service;

import com.kailash.storageservice.exception.FileNotFound;
import com.kailash.storageservice.exception.FileValidationException;
import com.kailash.storageservice.exception.StorageException;
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
import java.util.UUID;

@Service
public class FileService {
    private final Path storagePath= Paths.get("storage");

    public String uploadFile(MultipartFile file){
        final long MAX_FILE_SIZE = 100 * 1024 * 1024;

        if(file.isEmpty()){
            throw new FileValidationException("File cannot be empty");
        }

        if(file.getSize()>MAX_FILE_SIZE){
            throw new FileValidationException("File size limit exceeded!! (Must be 100MB or below)");
        }
        List<String> allowedTypes = List.of(
                "application/pdf",
                "image/jpeg",
                "image/png",
                "video/mp4"
        );
        if(!allowedTypes.contains(file.getContentType())){
            throw new FileValidationException("Unsupported file type");
        }
        String originalFileName=file.getOriginalFilename();
        String extension="";
        if(originalFileName!=null && originalFileName.contains(".")){
            extension=originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uniqueFileName= UUID.randomUUID()+extension;
        Path filePath=storagePath.resolve(uniqueFileName);
//        Files.copy(file.getInputStream(), filePath);
        try {
            Files.copy(file.getInputStream(), filePath);
        }
        catch (IOException ex){
            throw new StorageException("Failed to store file", ex);
        }
        return uniqueFileName;
    }

    public Resource downloadFile(String filename){
        try{
            Path filePath=storagePath.resolve(filename).normalize();
            Resource resource=new UrlResource(filePath.toUri());
            if(!resource.exists() || !resource.isReadable()){
                throw new FileNotFound("File not found: "+filename);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new StorageException("Unable to access file"+e);
        }
    }

    public void deleteFile(String filename){
        Path filePath=storagePath.resolve(filename).normalize();
        try{
            if(!Files.exists(filePath)){
                throw new FileNotFound("File not found: "+filename);
            }
            Files.delete(filePath);
        } catch (IOException e) {
            throw new StorageException("Failed to delete file: "+e);
        }
    }
}
