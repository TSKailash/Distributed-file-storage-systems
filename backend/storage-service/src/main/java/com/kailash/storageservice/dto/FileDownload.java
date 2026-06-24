package com.kailash.storageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.io.Resource;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileDownload {
    private Resource resource;
    private String originalName;
    private String contentType;
}
