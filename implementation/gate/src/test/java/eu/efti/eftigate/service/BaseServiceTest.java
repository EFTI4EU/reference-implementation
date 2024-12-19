package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersResultsDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.dto.SearchParameter;
import eu.efti.commons.dto.UilDto;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.MemoryAppender;
import eu.efti.edeliveryapconnector.service.RequestUpdaterService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.IdentifiersResults;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.service.gate.EftiGateIdResolver;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.entity.UsedTransportEquipment;
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
    protected EftiGateIdResolver eftiGateIdResolver;

    protected final UilDto uilDto = new UilDto();
    protected final ControlDto controlDto = new ControlDto();
    protected final ControlDto savedControlDto = new ControlDto();

    protected final ControlEntity controlEntityError = new ControlEntity();
    protected final ControlEntity controlEntity = new ControlEntity();

    protected final RequestDto requestDto = new RequestDto();
    protected final Consignment identifiersResult = new Consignment();
    protected final ConsignmentDto identifiersResultDto = new ConsignmentDto();
    protected final IdentifiersResults identifiersResults = new IdentifiersResults();
    protected final IdentifiersResultsDto identifiersResultsDto = new IdentifiersResultsDto();

    protected final SearchParameter searchParameter = new SearchParameter();

    public void before() {
        gateProperties = GateProperties.builder().ap(GateProperties.ApConfig.builder().url("url").password("pwd").username("usr").build()).owner("owner").build();

        final LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC);
        final String requestId = "67fe38bd-6bf7-4b06-b20e-206264bd639c";

        this.uilDto.setGateId("gate");
        this.uilDto.setDatasetId("uuid");
        this.uilDto.setPlatformId("plateform");

        searchParameter.setIdentifier("AA123VV");
        searchParameter.setRegistrationCountryCode(CountryIndicator.BE.toString());
        searchParameter.setModeCode("1");

        this.controlDto.setEftiDataUuid(uilDto.getDatasetId());
        this.controlDto.setGateId(uilDto.getGateId());
        this.controlDto.setPlatformId(uilDto.getPlatformId());
        this.controlDto.setRequestId(requestId);
        this.controlDto.setRequestType(RequestTypeEnum.LOCAL_UIL_SEARCH);
        this.controlDto.setStatus(StatusEnum.PENDING);
        this.controlDto.setSubsetId("oki");
        this.controlDto.setCreatedDate(localDateTime);
        this.controlDto.setLastModifiedDate(localDateTime);

        savedControlDto.setEftiDataUuid(uilDto.getDatasetId());
        savedControlDto.setGateId(uilDto.getGateId());
        savedControlDto.setPlatformId(uilDto.getPlatformId());
        savedControlDto.setRequestId("42");
        savedControlDto.setRequestType(RequestTypeEnum.EXTERNAL_UIL_SEARCH);
        savedControlDto.setSubsetId("oki");
        savedControlDto.setCreatedDate(LocalDateTime.now());
        savedControlDto.setLastModifiedDate(LocalDateTime.now());

        this.controlEntityError.setRequestId(requestId);
        this.controlEntityError.setRequestType(RequestTypeEnum.NOTE_SEND);
        this.controlEntityError.setStatus(StatusEnum.PENDING);
        this.controlDto.setSubsetId("oki");
        this.controlEntityError.setCreatedDate(localDateTime);
        this.controlEntityError.setLastModifiedDate(localDateTime);

        this.controlEntity.setEftiDataUuid(controlDto.getEftiDataUuid());
        this.controlEntity.setRequestId(controlDto.getRequestId());
        this.controlEntity.setRequestType(controlDto.getRequestType());
        this.controlEntity.setStatus(controlDto.getStatus());
        this.controlEntity.setPlatformId(controlDto.getPlatformId());
        this.controlEntity.setGateId(controlDto.getGateId());
        this.controlEntity.setSubsetId(controlDto.getSubsetId());
        this.controlEntity.setCreatedDate(controlDto.getCreatedDate());
        this.controlEntity.setLastModifiedDate(controlDto.getLastModifiedDate());
        this.controlEntity.setFromGateId(controlDto.getFromGateId());

        identifiersResult.setGateId("france");
        identifiersResult.setDatasetId("12345678-ab12-4ab6-8999-123456789abc");
        identifiersResult.setPlatformId("ttf");
        identifiersResult.setUsedTransportEquipments(List.of(UsedTransportEquipment.builder()
                        .equipmentId("vehicleId1")
                        .registrationCountry(CountryIndicator.FR.name())
                        .build(),
                UsedTransportEquipment.builder()
                        .equipmentId("vehicleId2")
                        .registrationCountry(CountryIndicator.CY.name())
                        .build()));
        identifiersResults.setConsignments(Collections.singletonList(identifiersResultDto));

        identifiersResultsDto.setConsignments(Collections.singletonList(identifiersResultDto));
    }

    protected <T extends RequestEntity> void setEntityRequestCommonAttributes(final T requestEntity) {
        requestEntity.setStatus(this.requestDto.getStatus());
        requestEntity.setRetry(this.requestDto.getRetry());
        requestEntity.setCreatedDate(LocalDateTime.now());
        requestEntity.setGateIdDest(this.requestDto.getGateIdDest());
        requestEntity.setControl(controlEntity);
    }

    protected <T extends RequestEntity> void setEntityRequestCommonAttributesError(final T requestEntity) {
        requestEntity.setStatus(this.requestDto.getStatus());
        requestEntity.setRetry(this.requestDto.getRetry());
        requestEntity.setCreatedDate(LocalDateTime.now());
        requestEntity.setControl(controlEntityError);
    }

    protected <T extends RequestDto> void setDtoRequestCommonAttributes(final T requestDto) {
        requestDto.setStatus(RequestStatusEnum.RECEIVED);
        requestDto.setRetry(0);
        requestDto.setCreatedDate(LocalDateTime.now());
        requestDto.setGateIdDest(controlEntity.getGateId());
        requestDto.setControl(ControlDto.builder().id(1).build());
    }
}