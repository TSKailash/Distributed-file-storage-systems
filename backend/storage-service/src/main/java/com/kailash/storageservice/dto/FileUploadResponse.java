package com.kailash.storageservice.dto;

import java.util.UUID;

public class FileUploadResponse {
    private String message;
    private UUID fieldId;

    public FileUploadResponse(String message, UUID fieldId){
        this.message=message;
        this.fieldId=fieldId;
    }

    public String getMessage(){
        return message;
    }

    public UUID getFieldId(){
        return fieldId;
    }
}
