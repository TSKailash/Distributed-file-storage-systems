package com.kailash.storageservice.controller;

import com.kailash.storageservice.dto.FileDownload;
import com.kailash.storageservice.dto.FileResponse;
import com.kailash.storageservice.dto.FileUploadResponse;
import com.kailash.storageservice.dto.UserDTO;
import com.kailash.storageservice.model.FileMetadata;
import com.kailash.storageservice.model.User;
import com.kailash.storageservice.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

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
        UUID id=fileService.uploadFile(file);

        return new FileUploadResponse("File uploaded successfully", id);
    }

    @GetMapping("/me")
    public UserDTO getUser(){
        return fileService.getUser();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID id) {

        FileDownload file = fileService.downloadFile(id);

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getOriginalName() + "\""
                )
                .body(file.getResource());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable UUID id){
        fileService.deleteFile(id);
        return ResponseEntity.ok("Deleted successfully");
    }

    @GetMapping("/")
    public List<FileResponse> getAllFiles(@RequestParam (required = false, defaultValue = "0") int pageNo,
                                          @RequestParam (required = false, defaultValue = "5") int pageSize,
                                          @RequestParam (required = false, defaultValue = "id") String sortBy,
                                          @RequestParam (required = false) String sortDir,
                                          @RequestParam (required = false) String search){
        Sort sort=sortDir!=null && sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return fileService.getAllFiles(PageRequest.of(pageNo, pageSize, sort), search);
    }
}
