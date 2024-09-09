package eu.efti.eftigate.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ErrorDto;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.eftigate.entity.IdentifiersResults;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(
        generator = PropertyGenerator.class,
        property = "id")
public class RabbitRequestDto {
    private long id;
    private RequestStatusEnum status;
    private String edeliveryMessageId;
    private Integer retry;
    private byte[] reponseData;
    private LocalDateTime nextRetryDate;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private String gateUrlDest;
    private ControlDto control;
    private ErrorDto error;
    private IdentifiersResults identifiersResults;
    private RequestType requestType;
    private String note;
    @JsonProperty("eFTIPlatformUrl")
    private String eFTIPlatformUrl;
}
