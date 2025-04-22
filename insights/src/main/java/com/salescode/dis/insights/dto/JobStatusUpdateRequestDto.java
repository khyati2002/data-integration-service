package com.salescode.dis.insights.dto;


import jakarta.validation.constraints.NotBlank;

public class JobStatusUpdateRequestDto {
    
    @NotBlank(message = "Status cannot be blank")
    private String status;
    
    // Getters and setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}