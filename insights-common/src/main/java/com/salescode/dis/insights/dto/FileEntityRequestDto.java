package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.AssertTrue;
import java.io.Serializable;
import java.util.Optional;

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
    Boolean isApiBased = Boolean.FALSE;

    @AssertTrue(message = "Either set the total, or if its api based then total count should not be set, it will be calculated from progress")
    public boolean hasTotalCount() {
        Boolean isApiBased = Optional.ofNullable(this.isApiBased).orElse(false);
        return (!isApiBased && totalCount != null) || (isApiBased && totalCount == null);
    }

}
