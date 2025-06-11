package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.AssertTrue;
import java.io.Serializable;
import java.util.Optional;
import com.salescode.dis.insights.enums.IntegrationMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FileEntityRequestDto implements Serializable {
    String fileId;
    JsonNode extendedAttributes;
    @Builder.Default
    Long totalCount = 0L;
    @Builder.Default
    IntegrationMode modeOfIntegration = IntegrationMode.FILE;

    @AssertTrue(message = "Either set the total, or if its API based then total count should not be set, it will be calculated from progress")
    public boolean hasTotalCount() {
        boolean isFileBased = Optional.ofNullable(this.modeOfIntegration).orElse(IntegrationMode.FILE).equals(IntegrationMode.FILE);
        return (isFileBased && totalCount != null) || (!isFileBased && (totalCount == null || totalCount <= 0));
    }

}
