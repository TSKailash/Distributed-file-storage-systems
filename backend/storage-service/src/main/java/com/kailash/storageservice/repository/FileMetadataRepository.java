package com.kailash.storageservice.repository;

import com.kailash.storageservice.model.FileMetaData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository extends JpaRepository<FileMetaData, UUID>{
    Optional<FileMetaData> findByStoredName(String filename);
}
