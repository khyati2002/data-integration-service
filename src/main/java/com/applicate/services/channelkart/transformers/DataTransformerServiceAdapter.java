package com.applicate.services.channelkart.transformers;

import com.applicate.services.channelkart.exceptions.checked.ConfigurationException;
import org.springframework.stereotype.Service;

@Service
public class DataTransformerServiceAdapter<S, T> implements DataTransformerServiceInterface {

    private DataTransformerService dataTransformerService;

    DataTransformerServiceAdapter(DataTransformerService dataTransformerServiceInterface){
        this.dataTransformerService=dataTransformerServiceInterface;
    }

    @Override
    public Object transformByName(String s, String s1, Object s2) {
        return dataTransformerService.transformByName(s,s1,s2);
    }

    @Override
    public Object transformById(String s, String s1, Object s2) {
        try {
            return dataTransformerService.transformById(s,s1,s2);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
