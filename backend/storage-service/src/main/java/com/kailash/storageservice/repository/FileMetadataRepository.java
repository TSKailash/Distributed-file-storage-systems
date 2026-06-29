package com.kailash.storageservice.repository;

import com.kailash.storageservice.model.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID>{
    Optional<FileMetadata> findByStoredName(String filename);
    Page<FileMetadata> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<FileMetadata> findByOwnerIdAndOriginalNameContaining(
            UUID ownerId,
            Pageable pageable,
            String originalName
    );
//    Page<FileMetadata> findByOwnerIdAndOriginalName(UUID ownerId, Pageable pageable);
}
