package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersResultDto;
import eu.efti.commons.dto.IdentifiersResultsDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.dto.SearchParameter;
import eu.efti.commons.dto.UilDto;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.enums.TransportMode;
import eu.efti.edeliveryapconnector.service.RequestUpdaterService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.IdentifiersResult;
import eu.efti.eftigate.entity.IdentifiersResults;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.service.gate.EftiGateUrlResolver;
import eu.efti.identifiersregistry.entity.TransportVehicle;
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
    @Mock
    protected LogManager logManager;
    @Mock
    protected EftiGateUrlResolver eftiGateUrlResolver;

    protected final UilDto uilDto = new UilDto();
    protected final ControlDto controlDto = new ControlDto();
    protected final ControlEntity controlEntity = new ControlEntity();

    protected final RequestDto requestDto = new RequestDto();
    protected final IdentifiersResult identifiersResult = new IdentifiersResult();
    protected final IdentifiersResults identifiersResults = new IdentifiersResults();
    protected final IdentifiersResultDto identifiersResultDto = new IdentifiersResultDto();
    protected final IdentifiersResultsDto identifiersResultsDto = new IdentifiersResultsDto();
    protected final TransportVehicle transportVehicle = new TransportVehicle();

    protected final SearchParameter searchParameter = new SearchParameter();

    public void before() {
        gateProperties = GateProperties.builder().ap(GateProperties.ApConfig.builder().url("url").password("pwd").username("usr").build()).owner("owner").build();

        final LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC);
        final String requestUuid = "67fe38bd-6bf7-4b06-b20e-206264bd639c";

        this.uilDto.setEFTIGateUrl("gate");
        this.uilDto.setEFTIDataUuid("uuid");
        this.uilDto.setEFTIPlatformUrl("plateform");

        searchParameter.setVehicleId("AA123VV");
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
        this.controlEntity.setEftiData(controlDto.getEftiData());
        this.controlEntity.setTransportIdentifiers(controlDto.getTransportIdentifiers());
        this.controlEntity.setFromGateUrl(controlDto.getFromGateUrl());

        identifiersResult.setCountryStart("FR");
        identifiersResult.setCountryEnd("FR");
        identifiersResult.setDisabled(false);
        identifiersResult.setDangerousGoods(true);
        identifiersResult.setTransportVehicles(List.of(transportVehicle));
        identifiersResults.setIdentifiersResult(Collections.singletonList(identifiersResult));

        identifiersResultDto.setCountryStart("FR");
        identifiersResultDto.setCountryEnd("FR");
        identifiersResultDto.setDisabled(false);
        identifiersResultDto.setDangerousGoods(true);
        identifiersResultsDto.setIdentifiersResult(Collections.singletonList(identifiersResultDto));
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
