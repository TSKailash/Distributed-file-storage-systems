package com.kailash.storageservice.dto;

public class FileUploadResponse {
    private String message;
    private String filename;

    public FileUploadResponse(String message, String filename){
        this.message=message;
        this.filename=filename;
    }

    public String getMessage(){
        return message;
    }

    public String getFilename(){
        return filename;
    }
}
