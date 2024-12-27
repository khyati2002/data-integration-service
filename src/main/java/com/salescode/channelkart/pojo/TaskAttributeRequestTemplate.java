/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.salescode.channelkart.services.enums.OperationType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskAttributeRequestTemplate {

    private String entityName;

    private String filePath;

    private String fieldName;

    private Map<String, String> criteria;

    private String lob;

    private String type;

    private Collection<TransformerInfo> transformerInfo;

    @Setter
    public static class TransformerInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        @Getter
        private boolean skipPreprocessing;

        @Getter
        private boolean skipPersist;

        @Getter
        private String entityName;

        @Getter
        private String transformerId;

        @Getter
        private String preprocessValidationExcludeGroup;
        @Getter
        private String messageLevelHash;
        private boolean messageHashSupported;
        @Getter
        private String messageLevelKey;
        @Getter
        private String[] cachedArtifact;
        @Getter
        private com.salescode.channelkart.transformers.TransformerInfo transformer;
        @Getter
        private OperationType operationType = OperationType.insert;

        public boolean getMessageHashSupported() {
            return messageHashSupported;
        }

    }

}
