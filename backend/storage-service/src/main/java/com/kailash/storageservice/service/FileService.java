package com.kailash.storageservice.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileService {
    private final Path storagePath= Paths.get("storage");

    public String uploadFile(MultipartFile file) throws IOException{
        String originalFileName=file.getOriginalFilename();
        String extension="";
        if(originalFileName!=null && originalFileName.contains(".")){
            extension=originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uniqueFileName= UUID.randomUUID()+extension;
        Path filePath=storagePath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath);
        return uniqueFileName;
    }
}
