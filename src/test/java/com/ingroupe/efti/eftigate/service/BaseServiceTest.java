package com.ingroupe.efti.eftigate.service;

import com.ingroupe.common.test.log.MemoryAppender;
import com.ingroupe.efti.commons.dto.ControlDto;
import com.ingroupe.efti.commons.dto.MetadataResultDto;
import com.ingroupe.efti.commons.dto.MetadataResultsDto;
import com.ingroupe.efti.commons.dto.RequestDto;
import com.ingroupe.efti.commons.dto.SearchParameter;
import com.ingroupe.efti.commons.dto.UilDto;
import com.ingroupe.efti.commons.enums.CountryIndicator;
import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.enums.RequestTypeEnum;
import com.ingroupe.efti.commons.enums.StatusEnum;
import com.ingroupe.efti.commons.enums.TransportMode;
import com.ingroupe.efti.edeliveryapconnector.service.RequestUpdaterService;
import com.ingroupe.efti.eftigate.config.GateProperties;
import com.ingroupe.efti.eftigate.entity.ControlEntity;
import com.ingroupe.efti.eftigate.entity.MetadataResult;
import com.ingroupe.efti.eftigate.entity.MetadataResults;
import com.ingroupe.efti.eftigate.entity.RequestEntity;
import com.ingroupe.efti.eftigate.service.gate.EftiGateUrlResolver;
import com.ingroupe.efti.metadataregistry.entity.TransportVehicle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public abstract class BaseServiceTest extends AbstractServiceTest {
    protected static final String MESSAGE_ID = "messageId";

    @Mock
    protected RabbitSenderService rabbitSenderService;
    @Mock
    protected ControlService controlService;
    @Mock
    protected RequestUpdaterService requestUpdaterService;
    @Mock
    protected GateProperties gateProperties;
    protected MemoryAppender memoryAppender;
    @Mock
    protected LogManager logManager;
    @Mock
    protected EftiGateUrlResolver eftiGateUrlResolver;

    protected final UilDto uilDto = new UilDto();
    protected final ControlDto controlDto = new ControlDto();
    protected final ControlEntity controlEntity = new ControlEntity();

    protected final RequestDto requestDto = new RequestDto();
    protected final MetadataResult metadataResult = new MetadataResult();
    protected final MetadataResults metadataResults = new MetadataResults();
    protected final MetadataResultDto metadataResultDto = new MetadataResultDto();
    protected final MetadataResultsDto metadataResultsDto = new MetadataResultsDto();
    protected final TransportVehicle transportVehicle = new TransportVehicle();

    protected final SearchParameter searchParameter = new SearchParameter();

    public void before() {
        gateProperties = GateProperties.builder().ap(GateProperties.ApConfig.builder().url("url").password("pwd").username("usr").build()).owner("owner").build();

        final LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC);
        final String requestUuid = "67fe38bd-6bf7-4b06-b20e-206264bd639c";

        this.uilDto.setEFTIGateUrl("gate");
        this.uilDto.setEFTIDataUuid("uuid");
        this.uilDto.setEFTIPlatformUrl("plateform");

        searchParameter.setVehicleID("AA123VV");
        searchParameter.setVehicleCountry(CountryIndicator.BE.toString());
        searchParameter.setTransportMode(TransportMode.ROAD.toString());

        this.controlDto.setEftiDataUuid(uilDto.getEFTIDataUuid());
        this.controlDto.setEftiGateUrl(uilDto.getEFTIGateUrl());
        this.controlDto.setEftiPlatformUrl(uilDto.getEFTIPlatformUrl());
        this.controlDto.setRequestUuid(requestUuid);
        this.controlDto.setRequestType(RequestTypeEnum.LOCAL_UIL_SEARCH);
        this.controlDto.setStatus(StatusEnum.PENDING);
        this.controlDto.setSubsetEuRequested("oki");
        this.controlDto.setSubsetMsRequested("oki");
        this.controlDto.setCreatedDate(localDateTime);
        this.controlDto.setLastModifiedDate(localDateTime);

        this.controlEntity.setEftiDataUuid(controlDto.getEftiDataUuid());
        this.controlEntity.setRequestUuid(controlDto.getRequestUuid());
        this.controlEntity.setRequestType(controlDto.getRequestType());
        this.controlEntity.setStatus(controlDto.getStatus());
        this.controlEntity.setEftiPlatformUrl(controlDto.getEftiPlatformUrl());
        this.controlEntity.setEftiGateUrl(controlDto.getEftiGateUrl());
        this.controlEntity.setSubsetEuRequested(controlDto.getSubsetEuRequested());
        this.controlEntity.setSubsetMsRequested(controlDto.getSubsetMsRequested());
        this.controlEntity.setCreatedDate(controlDto.getCreatedDate());
        this.controlEntity.setLastModifiedDate(controlDto.getLastModifiedDate());
        this.controlEntity.setTransportMetadata(controlDto.getTransportMetaData());
        this.controlEntity.setFromGateUrl(controlDto.getFromGateUrl());

        metadataResult.setCountryStart("FR");
        metadataResult.setCountryEnd("FR");
        metadataResult.setDisabled(false);
        metadataResult.setIsDangerousGoods(true);
        metadataResult.setTransportVehicles(List.of(transportVehicle));
        metadataResults.setMetadataResult(Collections.singletonList(metadataResult));

        metadataResultDto.setCountryStart("FR");
        metadataResultDto.setCountryEnd("FR");
        metadataResultDto.setDisabled(false);
        metadataResultDto.setIsDangerousGoods(true);
        metadataResultsDto.setMetadataResult(Collections.singletonList(metadataResultDto));
    }

    protected <T extends RequestEntity> void setEntityRequestCommonAttributes(final T requestEntity) {
        requestEntity.setStatus(this.requestDto.getStatus());
        requestEntity.setRetry(this.requestDto.getRetry());
        requestEntity.setCreatedDate(LocalDateTime.now());
        requestEntity.setGateUrlDest(this.requestDto.getGateUrlDest());
        requestEntity.setControl(controlEntity);
    }

    protected <T extends RequestDto> void setDtoRequestCommonAttributes(final T requestDto) {
        requestDto.setStatus(RequestStatusEnum.RECEIVED);
        requestDto.setRetry(0);
        requestDto.setCreatedDate(LocalDateTime.now());
        requestDto.setGateUrlDest(controlEntity.getEftiGateUrl());
        requestDto.setControl(ControlDto.builder().id(1).build());
    }
}
