package com.kailash.storageservice.controller;

import com.kailash.storageservice.dto.FileUploadResponse;
import com.kailash.storageservice.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
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
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
        String filename=fileService.uploadFile(file);

        return new FileUploadResponse("Uploaded", filename);
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename){
        Resource file=fileService.downloadFile(filename);
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""+file.getFilename()+"\""
                )
                .body(file);
    }

    @DeleteMapping("/{filename}")
    public ResponseEntity<String> deleteFile(@PathVariable String filename){
        fileService.deleteFile(filename);
        return ResponseEntity.ok("Deleted successfully");
    }
}
