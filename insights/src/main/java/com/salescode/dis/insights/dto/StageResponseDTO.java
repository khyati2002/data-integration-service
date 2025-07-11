package com.salescode.dis.insights.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageResponseDTO {
    private String mode;
    private List<StageDto> stagesSupported;
}