package com.ingroupe.efti.commons.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
public class RequestDto {
    private long id;
    private RequestStatusEnum status;
    private String edeliveryMessageId;
    private Integer retry;
    private LocalDateTime nextRetryDate;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private String gateUrlDest;
    private ControlDto control;
    private ErrorDto error;
    private RequestType requestType;

    protected RequestDto(final ControlDto controlDto) {
        this.retry = 0;
        this.gateUrlDest = controlDto.getEftiGateUrl();
        this.control = controlDto;
    }

    public RequestDto(final ControlDto controlDto, final String destinationUrl) {
        this.status = RequestStatusEnum.RECEIVED;
        this.retry = 0;
        this.gateUrlDest = StringUtils.isEmpty(destinationUrl) ? controlDto.getEftiGateUrl() : destinationUrl;
        this.control = controlDto;
    }
}
