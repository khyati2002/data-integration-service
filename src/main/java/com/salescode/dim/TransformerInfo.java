package com.salescode.dim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransformerInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean skipPreprocessing;

    private boolean skipPersist;

    private String entityName;

    private String transformerId;

    private String preprocessValidationExcludeGroup;

    private String messageLevelHash;

    private boolean messageHashSupported;

    private String messageLevelKey;

    private String[] cachedArtifact;

    @Builder.Default
    private OperationType operationType = OperationType.insert;

    public boolean getMessageHashSupported() {
        return messageHashSupported;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType != null ? operationType : OperationType.insert;
    }

}