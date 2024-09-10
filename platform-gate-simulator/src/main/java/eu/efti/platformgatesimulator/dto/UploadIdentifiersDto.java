package eu.efti.platformgatesimulator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.efti.commons.dto.IdentifiersDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadIdentifiersDto {
    @JsonProperty("identifiersDto")
    private IdentifiersDto identifiersDto;
    private MultipartFile file;
}
