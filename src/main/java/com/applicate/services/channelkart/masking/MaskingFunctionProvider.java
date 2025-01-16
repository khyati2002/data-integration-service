package com.applicate.services.channelkart.masking;

import com.applicate.services.channelkart.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MaskingFunctionProvider {

    private final Map<String, MaskingFunction> functionMap;

    public MaskingFunctionProvider(List<MaskingFunction> functions) {
        this.functionMap = prepareFunctionMap(functions);
    }

    private Map<String, MaskingFunction> prepareFunctionMap(List<MaskingFunction> functions) {
        Map<String, MaskingFunction> maskingFunctions = new HashMap<>();
        for (MaskingFunction function : functions) {
            String identifier = function.identifier();
            if (StringUtils.isNullOrBlank(identifier)) {
                throw new IllegalStateException("Got empty identifier for " + identifier);
            }
            if (maskingFunctions.containsKey(identifier)) {
                throw new IllegalStateException("Found duplicate identifier " + identifier);
            }
            maskingFunctions.put(identifier, function);
        }
        return maskingFunctions;
    }

    public Optional<MaskingFunction> getFunction(String identifier) {
        return Optional.ofNullable(functionMap.get(identifier));
    }
}
