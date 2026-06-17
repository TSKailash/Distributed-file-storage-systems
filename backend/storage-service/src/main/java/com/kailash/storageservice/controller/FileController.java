package com.kailash.storageservice.controller;

import com.kailash.storageservice.dto.FileUploadResponse;
import com.kailash.storageservice.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService){
        this.fileService=fileService;
    }

    @PostMapping("/upload")
    public FileUploadResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
        String filename=fileService.uploadFile(file);
        return new FileUploadResponse("Uploaded", filename);
    }
}
