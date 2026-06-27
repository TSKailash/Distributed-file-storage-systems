package com.kailash.storageservice.repository;

import com.kailash.storageservice.model.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID>{
    Optional<FileMetadata> findByStoredName(String filename);
    Page<FileMetadata> findByOwnerId(UUID ownerId, Pageable pageable);
}
