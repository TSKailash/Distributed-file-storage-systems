package com.kailash.storageservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FileResponse(
        UUID id,
        String originalName,
        Long size,
        String contentType,
        LocalDateTime createdAt
) {}