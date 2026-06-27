package com.kailash.storageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CachedFileMetaData {
    private UUID id;
    private String originalName;
    private String storedName;
    private Long size;
    private String contentType;
    private LocalDateTime createdAt;
    private UUID ownerId;
}