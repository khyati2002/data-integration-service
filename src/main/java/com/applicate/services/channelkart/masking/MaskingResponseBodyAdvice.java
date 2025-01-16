package com.applicate.services.channelkart.masking;

import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ControllerAdvice
public class MaskingResponseBodyAdvice implements ResponseBodyAdvice<Object> {
    
    private final PropertyRegistry propertyRegistry;

    private final MaskingFunctionProvider maskingFunctionProvider;

    public MaskingResponseBodyAdvice(PropertyRegistry propertyRegistry, MaskingFunctionProvider maskingFunctionProvider) {
        this.propertyRegistry = propertyRegistry;
        this.maskingFunctionProvider = maskingFunctionProvider;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getMethodAnnotation(MaskResponse.class) != null ||
                returnType.getContainingClass().isAnnotationPresent(MaskResponse.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        MaskResponse methodAnnotation = returnType.getMethodAnnotation(MaskResponse.class);
        assert methodAnnotation != null;
        PropertyDefinition propertyDefinition = methodAnnotation.value();
        String maskingFunctions = propertyRegistry.getValue(propertyDefinition);
        if (StringUtils.isBlank(maskingFunctions)) {
//            no need to process
            return body;
        }
        List<MaskingFunction> functions = getFunctions(maskingFunctions);
        return executeFunctions(body, functions);

    }

    private Object executeFunctions(Object body, List<MaskingFunction> functions) {
        for (MaskingFunction function : functions) {
            body = function.mask(body);
        }
        return body;
    }

    private List<MaskingFunction> getFunctions(String maskingFunctions) {
        return Arrays.stream(maskingFunctions.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(maskingFunctionProvider::getFunction)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }


}
