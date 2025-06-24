package com.salescode.dis.insights.validation;

import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.JobEntity;
import org.springframework.stereotype.Service;

@Service
public class ValidationService {

    public void validate(FileEntity file, FileProgressRequest stage){
        if(file.getModeOfIntegration() != stage.getModeOfIntegration()) {
            throw new IllegalArgumentException("Mode does not match with the registed job.");
        }
    }
}
