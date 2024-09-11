package com.ingroupe.platform.platformgatesimulator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ingroupe.efti.commons.dto.MetadataDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadMetadataDto {
    @JsonProperty("metadataDto")
    private MetadataDto metadataDto;
    private MultipartFile file;
}
