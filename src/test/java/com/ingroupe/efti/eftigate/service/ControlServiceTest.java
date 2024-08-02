package com.ingroupe.efti.eftigate.service;

import com.ingroupe.efti.commons.dto.AuthorityDto;
import com.ingroupe.efti.commons.dto.ContactInformationDto;
import com.ingroupe.efti.commons.dto.ControlDto;
import com.ingroupe.efti.commons.dto.ErrorDto;
import com.ingroupe.efti.commons.dto.MetadataDto;
import com.ingroupe.efti.commons.dto.MetadataRequestDto;
import com.ingroupe.efti.commons.dto.MetadataResponseDto;
import com.ingroupe.efti.commons.dto.MetadataResultDto;
import com.ingroupe.efti.commons.dto.MetadataResultsDto;
import com.ingroupe.efti.commons.dto.NotesDto;
import com.ingroupe.efti.commons.dto.SearchParameter;
import com.ingroupe.efti.commons.dto.TransportVehicleDto;
import com.ingroupe.efti.commons.dto.UilDto;
import com.ingroupe.efti.commons.enums.ErrorCodesEnum;
import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.enums.RequestTypeEnum;
import com.ingroupe.efti.commons.enums.StatusEnum;
import com.ingroupe.efti.edeliveryapconnector.dto.IdentifiersMessageBodyDto;
import com.ingroupe.efti.eftigate.config.GateProperties;
import com.ingroupe.efti.eftigate.dto.NoteResponseDto;
import com.ingroupe.efti.eftigate.dto.RequestUuidDto;
import com.ingroupe.efti.eftigate.entity.ControlEntity;
import com.ingroupe.efti.eftigate.entity.IdentifiersRequestEntity;
import com.ingroupe.efti.eftigate.entity.MetadataResult;
import com.ingroupe.efti.eftigate.entity.MetadataResults;
import com.ingroupe.efti.eftigate.entity.RequestEntity;
import com.ingroupe.efti.eftigate.entity.UilRequestEntity;
import com.ingroupe.efti.eftigate.exception.AmbiguousIdentifierException;
import com.ingroupe.efti.eftigate.repository.ControlRepository;
import com.ingroupe.efti.eftigate.service.gate.EftiGateUrlResolver;
import com.ingroupe.efti.eftigate.service.request.MetadataRequestService;
import com.ingroupe.efti.eftigate.service.request.NotesRequestService;
import com.ingroupe.efti.eftigate.service.request.RequestServiceFactory;
import com.ingroupe.efti.eftigate.service.request.UilRequestService;
import com.ingroupe.efti.metadataregistry.service.MetadataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static com.ingroupe.efti.commons.enums.StatusEnum.COMPLETE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;


@ExtendWith(MockitoExtension.class)
class ControlServiceTest extends AbstractServiceTest {

    @Mock
    private ControlRepository controlRepository;
    @Mock
    private UilRequestService uilRequestService;
    @Mock
    private NotesRequestService notesRequestService;
    @Mock
    private MetadataRequestService metadataRequestService;
    @Mock
    private MetadataService metadataService;

    private ControlService controlService;
    @Mock
    private EftiGateUrlResolver eftiGateUrlResolver;

    @Mock
    private LogManager logManager;

    @Mock
    private RequestServiceFactory requestServiceFactory;

    @Mock
    private EftiAsyncCallsProcessor eftiAsyncCallsProcessor;

    @Mock
    private Function<List<String>, RequestTypeEnum> gateToRequestTypeFunction;


    private final UilDto uilDto = new UilDto();
    private final NotesDto notesDto = new NotesDto();
    private final MetadataRequestDto metadataRequestDto = new MetadataRequestDto();
    private final ControlDto controlDto = new ControlDto();
    MetadataDto metadataDto= new MetadataDto();
    TransportVehicleDto transportVehicleDto = new TransportVehicleDto();
    private final ControlEntity controlEntity = ControlEntity.builder().requestType(RequestTypeEnum.LOCAL_UIL_SEARCH).build();
    private final UilRequestEntity uilRequestEntity = new UilRequestEntity();
    private final IdentifiersRequestEntity identifiersRequestEntity = new IdentifiersRequestEntity();
    private final IdentifiersRequestEntity secondIdentifiersRequestEntity = new IdentifiersRequestEntity();


    MetadataResult metadataResult = new MetadataResult();
    MetadataResults metadataResults = new MetadataResults();

    private final MetadataResultDto metadataResultDto = new MetadataResultDto();
    private final MetadataResultsDto metadataResultsDto = new MetadataResultsDto();

    private final RequestUuidDto requestUuidDto = new RequestUuidDto();
    private final String requestUuid = UUID.randomUUID().toString();
    private final String metadataUuid = UUID.randomUUID().toString();

    private final static String url = "http://france.lol";
    private final static String password = "password";
    private final static String username = "username";

    @BeforeEach
    public void before() {
        final GateProperties gateProperties = GateProperties.builder()
                .owner("http://france.lol")
                .country("FR")
                .ap(GateProperties.ApConfig.builder()
                        .url(url)
                        .password(password)
                        .username(username).build()).build();
        controlService = new ControlService(controlRepository, eftiGateUrlResolver, metadataService, mapperUtils,
                requestServiceFactory, logManager, gateToRequestTypeFunction, eftiAsyncCallsProcessor,
                gateProperties );
        final LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC);
        final StatusEnum status = StatusEnum.PENDING;
        final AuthorityDto authorityDto = AuthorityDto.builder()
                .nationalUniqueIdentifier("national identifier")
                .name("Robert")
                .workingContact(ContactInformationDto.builder()
                        .email("toto@gmail.com")
                        .city("Acheville")
                        .buildingNumber("12")
                        .postalCode("62320")
                        .streetName("rue jean luc de la rue").build())
                .country("FR")
                .legalContact(ContactInformationDto.builder()
                        .email("toto@gmail.com")
                        .city("Acheville")
                        .buildingNumber("12")
                        .postalCode("62320")
                        .streetName("rue jean luc de la rue").build())
                .isEmergencyService(true).build();

        requestUuidDto.setRequestUuid(requestUuid);
        requestUuidDto.setStatus(status);

        this.uilDto.setEFTIGateUrl("http://www.gate.com");
        this.uilDto.setEFTIDataUuid("12345678-ab12-4ab6-8999-123456789abc");
        this.uilDto.setEFTIPlatformUrl("http://www.platform.com");
        this.uilDto.setAuthority(authorityDto);

        this.metadataRequestDto.setVehicleID("abc123");
        this.metadataRequestDto.setVehicleCountry("FR");
        this.metadataRequestDto.setAuthority(authorityDto);
        this.metadataRequestDto.setTransportMode("ROAD");

        this.controlDto.setEftiDataUuid(uilDto.getEFTIDataUuid());
        this.controlDto.setEftiGateUrl(uilDto.getEFTIGateUrl());
        this.controlDto.setEftiPlatformUrl(uilDto.getEFTIPlatformUrl());
        this.controlDto.setRequestUuid(requestUuid);
        this.controlDto.setRequestType(RequestTypeEnum.LOCAL_UIL_SEARCH);
        this.controlDto.setStatus(status);
        this.controlDto.setSubsetEuRequested("oki");
        this.controlDto.setSubsetMsRequested("oki");
        this.controlDto.setCreatedDate(localDateTime);
        this.controlDto.setLastModifiedDate(localDateTime);
        this.controlDto.setAuthority(AuthorityDto.builder()
                .country("FR")
                .isEmergencyService(true)
                .legalContact(ContactInformationDto.builder().build())
                .workingContact(ContactInformationDto.builder().build())
                .nationalUniqueIdentifier("unique").build());

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
        this.controlEntity.setTransportMetadata(controlDto.getTransportMetaData());
        this.controlEntity.setFromGateUrl(controlDto.getFromGateUrl());

        identifiersRequestEntity.setRequestType("IDENTIFIER");
        secondIdentifiersRequestEntity.setRequestType("IDENTIFIER");
        uilRequestEntity.setRequestType("UIL");


        metadataResult.setCountryStart("FR");
        metadataResult.setCountryEnd("FR");
        metadataResult.setDisabled(false);
        metadataResult.setDangerousGoods(true);

        metadataResults.setMetadataResult(Collections.singletonList(metadataResult));

        uilRequestEntity.setControl(controlEntity);


        transportVehicleDto.setCountryStart("FR");
        transportVehicleDto.setCountryEnd("FR");
        transportVehicleDto.setTransportMode("ROAD");
        transportVehicleDto.setJourneyStart(LocalDateTime.now().toString());
        transportVehicleDto.setJourneyStart(LocalDateTime.now().plusHours(5L).toString());

        metadataDto.setDangerousGoods(true);
        metadataDto.setMetadataUUID(metadataUuid);
        metadataDto.setDisabled(false);
        metadataDto.setCountryStart("FR");
        metadataDto.setCountryEnd("FR");
        metadataDto.setTransportVehicles(Collections.singletonList(transportVehicleDto));

        metadataResult.setCountryStart("FR");
        metadataResult.setCountryEnd("FR");
        metadataResult.setDisabled(false);
        metadataResult.setDangerousGoods(true);

        metadataResults.setMetadataResult(Collections.singletonList(metadataResult));

        metadataResultDto.setCountryStart("FR");
        metadataResultDto.setCountryEnd("FR");
        metadataResultDto.setDisabled(false);
        metadataResultDto.setDangerousGoods(true);

        metadataResultsDto.setMetadataResult(Collections.singletonList(metadataResultDto));

        setField(controlService,"timeoutValue", 60);
    }

    @Test
    void getByIdWithDataTest() {
        when(controlRepository.findById(1L)).thenReturn(Optional.of(new ControlEntity()));

        final ControlEntity savedControlEntity = controlService.getById(1L);

        verify(controlRepository, times(1)).findById(1L);
        assertNotNull(savedControlEntity);
    }

    @Test
    void createControlEntitySameGate() {
        uilDto.setEFTIGateUrl("http://france.lol");

        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);
        when(metadataService.existByUIL(any(), any(), any())).thenReturn(true);

        final RequestUuidDto requestUuidDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, times(1)).createAndSendRequest(any(), any());
        verify(controlRepository, times(1)).save(any());
        verify(logManager).logAppRequest(any(), any());
        verify(logManager, never()).logAppResponse(any(), any());
        assertNotNull(requestUuidDtoResult);
        assertNull(requestUuidDtoResult.getErrorCode());
        assertNull(requestUuidDtoResult.getErrorDescription());
    }

    @Test
    void createControlEntityTest() {
        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);

        final RequestUuidDto requestUuidDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, times(1)).createAndSendRequest(any(), any());
        verify(controlRepository, times(1)).save(any());
        verify(logManager).logAppRequest(any(), any());
        verify(logManager, never()).logAppResponse(any(), any());
        assertNotNull(requestUuidDtoResult);
        assertNull(requestUuidDtoResult.getErrorCode());
        assertNull(requestUuidDtoResult.getErrorDescription());
    }

    @Test
    void createControlEntityErrorGateNullTest() {
        uilDto.setEFTIGateUrl(null);
        controlDto.setEftiGateUrl(null);
        controlEntity.setStatus(StatusEnum.ERROR);

        final RequestUuidDto requestUuidDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(controlRepository, never()).save(any());
        verify(logManager).logAppRequest(any(), any());
        verify(logManager).logAppResponse(any(), any());
        assertNotNull(requestUuidDtoResult);
        assertEquals(ErrorCodesEnum.UIL_GATE_MISSING.name(), requestUuidDtoResult.getErrorCode());
        assertEquals("Missing parameter eFTIGateUrl", requestUuidDtoResult.getErrorDescription());
    }

    @Test
    void createControlEntityErrorGateFormatTest() {
        uilDto.setEFTIGateUrl("toto");
        controlEntity.setStatus(StatusEnum.ERROR);

        final RequestUuidDto requestUuidDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(controlRepository, times(0)).save(any());
        verify(logManager).logAppRequest(any(), any());
        verify(logManager).logAppResponse(any(), any());
        assertNotNull(requestUuidDtoResult);
        assertEquals(ErrorCodesEnum.UIL_GATE_INCORRECT_FORMAT.name(), requestUuidDtoResult.getErrorCode());
        assertEquals("Gate format incorrect.", requestUuidDtoResult.getErrorDescription());
    }

    @Test
    void createControlEntityErrorPlatformNullTest() {
        uilDto.setEFTIPlatformUrl(null);
        controlDto.setEftiPlatformUrl(null);
        controlEntity.setStatus(StatusEnum.ERROR);

        final RequestUuidDto requestUuidDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(controlRepository, never()).save(any());
        verify(logManager).logAppRequest(any(), any());
        verify(logManager).logAppResponse(any(), any());
        assertNotNull(requestUuidDtoResult);
        assertEquals(ErrorCodesEnum.UIL_PLATFORM_MISSING.name(), requestUuidDtoResult.getErrorCode());
        assertEquals("Missing parameter eFTIPlatformUrl", requestUuidDtoResult.getErrorDescription());
    }

    @Test
    void createControlEntityErrorPlatformFormatTest() {
        uilDto.setEFTIPlatformUrl("toto");
        controlEntity.setStatus(StatusEnum.ERROR);

        final RequestUuidDto requestUuidDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(controlRepository, times(0)).save(any());
        verify(logManager).logAppRequest(any(), any());
        verify(logManager).logAppResponse(any(), any());
        assertNotNull(requestUuidDtoResult);
        assertEquals(ErrorCodesEnum.UIL_PLATFORM_INCORRECT_FORMAT.name(), requestUuidDtoResult.getErrorCode());
        assertEquals("Platform format incorrect.", requestUuidDtoResult.getErrorDescription());
    }

    @Test
    void createControlEntityErrorUuidNullTest() {
        uilDto.setEFTIDataUuid(null);
        controlDto.setRequestUuid(null);
        controlEntity.setStatus(StatusEnum.ERROR);

        final RequestUuidDto requestUuidDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(controlRepository, never()).save(any());
        verify(logManager).logAppRequest(any(), any());
        verify(logManager).logAppResponse(any(), any());
        assertNotNull(requestUuidDtoResult);
        assertEquals(ErrorCodesEnum.UIL_UUID_MISSING.name(), requestUuidDtoResult.getErrorCode());
        assertEquals("Missing parameter eFTIDataUuid", requestUuidDtoResult.getErrorDescription());
    }

    @Test
    void createControlEntityErrorUuidFormatTest() {
        uilDto.setEFTIDataUuid("toto");
        controlEntity.setStatus(StatusEnum.ERROR);

        final RequestUuidDto requestUuidDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(controlRepository, times(0)).save(any());
        verify(logManager).logAppRequest(any(), any());
        verify(logManager).logAppResponse(any(), any());
        assertNotNull(requestUuidDtoResult);
        assertEquals(ErrorCodesEnum.UIL_UUID_INCORRECT_FORMAT.name(), requestUuidDtoResult.getErrorCode());
        assertEquals("Uuid format incorrect.", requestUuidDtoResult.getErrorDescription());
    }

    @Test
    void getControlEntitySuccessTest() {
        uilRequestEntity.setStatus(RequestStatusEnum.SUCCESS);
        controlEntity.setRequests(Collections.singletonList(uilRequestEntity));
        when(controlRepository.findByRequestUuid(any())).thenReturn(Optional.of(controlEntity));
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);
        when(controlRepository.save(any())).thenReturn(controlEntity);

        final RequestUuidDto requestUuidDtoResult = controlService.getControlEntity(requestUuid);

        verify(controlRepository, times(1)).findByRequestUuid(any());

        verify(logManager).logAppResponse(any(), any());
        assertNotNull(requestUuidDtoResult);
        assertEquals(requestUuidDtoResult.getRequestUuid(), controlEntity.getRequestUuid());
    }

    @Test
    void getControlEntitySuccessTestWhenStatusComplete() {
        controlEntity.setStatus(COMPLETE);
        when(controlRepository.findByRequestUuid(any())).thenReturn(Optional.of(controlEntity));

        final RequestUuidDto requestUuidDtoResult = controlService.getControlEntity(requestUuid);

        verify(controlRepository, times(1)).findByRequestUuid(any());
        verify(metadataRequestService, never()).setDataFromRequests(any());
        verify(uilRequestService, never()).setDataFromRequests(any());

        verify(logManager).logAppResponse(any(), any());
        assertNotNull(requestUuidDtoResult);
        assertEquals(requestUuidDtoResult.getRequestUuid(), controlEntity.getRequestUuid());
    }

    @Test
    void shouldUpdatePendingControl_whenMetadataSearchAndRequestsContainsData() {
        //Arrange
        controlEntity.setRequestType(RequestTypeEnum.LOCAL_METADATA_SEARCH);
        controlEntity.setRequests(Collections.singletonList(identifiersRequestEntity));
        identifiersRequestEntity.setMetadataResults(metadataResults);
        identifiersRequestEntity.setStatus(RequestStatusEnum.SUCCESS);

        when(controlRepository.findByRequestUuid(any())).thenReturn(Optional.of(controlEntity));
        when(metadataRequestService.allRequestsContainsData(any())).thenReturn(true);
        when(controlRepository.save(controlEntity)).thenReturn(controlEntity);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(metadataRequestService);

        //Act
        controlService.getControlEntity(requestUuid);

        assertEquals(COMPLETE, controlEntity.getStatus());

        verify(controlRepository, times(1)).save(controlEntity);
        verify(controlRepository, times(1)).findByRequestUuid(any());
        verify(metadataRequestService, times(1)).allRequestsContainsData(controlEntity.getRequests());
        verify(uilRequestService, never()).allRequestsContainsData(any());
    }

    @Test
    void shouldUpdatePendingControl_whenUilSearchAndRequestsContainsData() {
        //Arrange
        controlEntity.setRequestType(RequestTypeEnum.LOCAL_UIL_SEARCH);
        controlEntity.setRequests(Collections.singletonList(uilRequestEntity));

        final byte[] data = {10, 20, 30, 40};
        uilRequestEntity.setReponseData(data);
        uilRequestEntity.setStatus(RequestStatusEnum.SUCCESS);

        when(controlRepository.findByRequestUuid(any())).thenReturn(Optional.of(controlEntity));
        when(uilRequestService.allRequestsContainsData(any())).thenReturn(true);
        when(controlRepository.save(controlEntity)).thenReturn(controlEntity);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);

        //Act
        controlService.getControlEntity(requestUuid);

        //Assert
        assertEquals(COMPLETE, controlEntity.getStatus());

        verify(controlRepository, times(1)).save(controlEntity);
        verify(controlRepository, times(1)).findByRequestUuid(any());
        verify(uilRequestService, times(1)).allRequestsContainsData(controlEntity.getRequests());
        verify(metadataRequestService, never()).allRequestsContainsData(any());
    }

    @Test
    void getControlEntityNotFoundTest() {
        when(controlRepository.findByRequestUuid(any())).thenReturn(Optional.empty());

        final RequestUuidDto requestUuidDtoResult = controlService.getControlEntity(requestUuid);

        verify(controlRepository, times(1)).findByRequestUuid(any());
        assertNotNull(requestUuidDtoResult);
        assertEquals(StatusEnum.ERROR, requestUuidDtoResult.getStatus());
        assertNull(requestUuidDtoResult.getEFTIData());
    }

    @Test
    void shouldSetError() {
        final String description = "description";
        final String code = "code";
        final ErrorDto errorDto = ErrorDto.builder()
                .errorDescription(description)
                .errorCode(code).build();
        final ArgumentCaptor<ControlEntity> argumentCaptor = ArgumentCaptor.forClass(ControlEntity.class);
        when(controlRepository.save(any())).thenReturn(controlEntity);

        controlService.setError(controlDto, errorDto);

        verify(controlRepository).save(argumentCaptor.capture());
        assertEquals(StatusEnum.ERROR, argumentCaptor.getValue().getStatus());
    }

    @Test
    void createMetadataControlTest() {
        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(metadataRequestService);
        when(gateToRequestTypeFunction.apply(any())).thenReturn(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH);
        when(eftiGateUrlResolver.resolve(any(MetadataRequestDto.class))).thenReturn(List.of("http://efti.gate.borduria.eu"));


        final RequestUuidDto requestUuidDtoResult = controlService.createMetadataControl(metadataRequestDto);
        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(metadataRequestService, times(1)).createAndSendRequest(any(), any());
        verify(eftiAsyncCallsProcessor, never()).checkLocalRepoAsync(any(), any());
        verify(controlRepository, times(1)).save(any());
        assertNotNull(requestUuidDtoResult);
        assertNull(requestUuidDtoResult.getErrorCode());
        assertNull(requestUuidDtoResult.getErrorDescription());
    }

    @Test
    void createMetadataControlForLocalRequestTest() {
        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(gateToRequestTypeFunction.apply(any())).thenReturn(RequestTypeEnum.LOCAL_METADATA_SEARCH);
        when(eftiGateUrlResolver.resolve(any(MetadataRequestDto.class))).thenReturn(List.of("http://france.lol"));


        final RequestUuidDto requestUuidDtoResult = controlService.createMetadataControl(metadataRequestDto);
        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(metadataRequestService, never()).createAndSendRequest(any(), any());
        verify(eftiAsyncCallsProcessor, times(1)).checkLocalRepoAsync(any(), any());
        verify(controlRepository, times(1)).save(any());
        assertNotNull(requestUuidDtoResult);
        assertNull(requestUuidDtoResult.getErrorCode());
        assertNull(requestUuidDtoResult.getErrorDescription());
    }

    @Test
    void shouldCreateMetadataControlWithPendingStatus_whenSomeOfGivenDestinationGatesDoesNotExist() {
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_METADATA_SEARCH);
        metadataRequestDto.setEFTIGateIndicator(List.of("IT", "RO"));
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(metadataRequestService);
        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(gateToRequestTypeFunction.apply(any())).thenReturn(RequestTypeEnum.EXTERNAL_METADATA_SEARCH);
        when(eftiGateUrlResolver.resolve(any(MetadataRequestDto.class))).thenReturn(List.of("http://italie.it"));



        final RequestUuidDto requestUuidDtoResult = controlService.createMetadataControl(metadataRequestDto);
        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(metadataRequestService, times(1)).createAndSendRequest(any(), any());
        verify(eftiAsyncCallsProcessor, never()).checkLocalRepoAsync(any(), any());
        verify(controlRepository, times(1)).save(any());
        assertNotNull(requestUuidDtoResult);
        assertEquals(StatusEnum.PENDING, requestUuidDtoResult.getStatus());
    }

    @Test
    void  shouldCreateMetadataControlWithPendingStatus_whenAllGivenDestinationGatesDoesNotExist() {
        metadataRequestDto.setEFTIGateIndicator(List.of("IT", "RO"));
        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(gateToRequestTypeFunction.apply(any())).thenReturn(RequestTypeEnum.EXTERNAL_METADATA_SEARCH);


        final RequestUuidDto requestUuidDtoResult = controlService.createMetadataControl(metadataRequestDto);
        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(metadataRequestService, never()).createAndSendRequest(any(), any());
        verify(eftiAsyncCallsProcessor, never()).checkLocalRepoAsync(any(), any());
        verify(controlRepository, times(1)).save(any());
        assertNotNull(requestUuidDtoResult);
        assertEquals(StatusEnum.PENDING, requestUuidDtoResult.getStatus());
    }

    @Test
    void createMetadataControlWithMinimumRequiredTest() {
        metadataRequestDto.setTransportMode(null);
        metadataRequestDto.setVehicleCountry(null);
        metadataRequestDto.setEFTIGateIndicator(null);
        metadataRequestDto.setIsDangerousGoods(null);

        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(gateToRequestTypeFunction.apply(any())).thenReturn(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH);
        when(eftiGateUrlResolver.resolve(any(MetadataRequestDto.class))).thenReturn(List.of("http://efti.gate.borduria.eu"));
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(metadataRequestService);


        final RequestUuidDto requestUuidDtoResult = controlService.createMetadataControl(metadataRequestDto);

        verify(metadataRequestService, times(1)).createAndSendRequest(any(), any());
        verify(uilRequestService, times(0)).createAndSendRequest(any(), any());
        verify(controlRepository, times(1)).save(any());
        assertNotNull(requestUuidDtoResult);
        assertNull(requestUuidDtoResult.getErrorCode());
        assertNull(requestUuidDtoResult.getErrorDescription());
    }

    @Test
    void createMetadataControlVehicleIDIncorrect() {
        metadataRequestDto.setVehicleID("fausse plaque");

        final RequestUuidDto requestUuidDtoResult = controlService.createMetadataControl(metadataRequestDto);

        verify(uilRequestService, times(0)).createAndSendRequest(any(), any());
        verify(controlRepository, times(0)).save(any());
        assertNotNull(requestUuidDtoResult);
        assertEquals(ErrorCodesEnum.VEHICLE_ID_INCORRECT_FORMAT.name(), requestUuidDtoResult.getErrorCode());
        assertEquals(ErrorCodesEnum.VEHICLE_ID_INCORRECT_FORMAT.getMessage(),requestUuidDtoResult.getErrorDescription());
    }

    @Test
    void createMetadataControlVehicleCountryIncorrect() {
        metadataRequestDto.setVehicleCountry("toto");

        final RequestUuidDto requestUuidDtoResult = controlService.createMetadataControl(metadataRequestDto);

        verify(uilRequestService, times(0)).createAndSendRequest(any(), any());
        verify(controlRepository, times(0)).save(any());
        assertNotNull(requestUuidDtoResult);
        assertEquals(ErrorCodesEnum.VEHICLE_COUNTRY_INCORRECT.name(), requestUuidDtoResult.getErrorCode());
        assertEquals(ErrorCodesEnum.VEHICLE_COUNTRY_INCORRECT.getMessage(),requestUuidDtoResult.getErrorDescription());
    }

    @Test
    void createMetadataControlTransportModeIncorrect() {
        metadataRequestDto.setTransportMode("toto");

        final RequestUuidDto requestUuidDtoResult = controlService.createMetadataControl(metadataRequestDto);

        verify(uilRequestService, times(0)).createAndSendRequest(any(), any());
        verify(controlRepository, times(0)).save(any());
        assertNotNull(requestUuidDtoResult);
        assertEquals(ErrorCodesEnum.TRANSPORT_MODE_INCORRECT.name(), requestUuidDtoResult.getErrorCode());
        assertEquals(ErrorCodesEnum.TRANSPORT_MODE_INCORRECT.getMessage(),requestUuidDtoResult.getErrorDescription());
    }

    @Test
    void shouldCreateControlFromNotificationDtoAndMessageBody() {
        //Arrange
        final ControlEntity metadataControl = ControlEntity.builder()
                .id(1)
                .requestUuid("67fe38bd-6bf7-4b06-b20e-206264bd639c")
                .status(StatusEnum.PENDING)
                .requestType(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH)
                .subsetEuRequested("SubsetEuRequested")
                .subsetMsRequested("SubsetMsRequested")
                .eftiGateUrl("france")
                .fromGateUrl("https://efti.gate.france.eu")
                .transportMetadata(SearchParameter.builder()
                        .vehicleId("AA123VV")
                        .transportMode("ROAD")
                        .vehicleCountry("FR")
                        .isDangerousGoods(true)
                        .build())
                .build();
        final IdentifiersMessageBodyDto messageBodyDto = IdentifiersMessageBodyDto.builder()
                .transportMode("ROAD")
                .requestUuid("67fe38bd-6bf7-4b06-b20e-206264bd639c")
                .vehicleCountry("FR")
                .vehicleID("AA123VV")
                .isDangerousGoods(true)
                .build();
        final ControlDto expectedControl = ControlDto.builder()
                .requestUuid("67fe38bd-6bf7-4b06-b20e-206264bd639c")
                .status(StatusEnum.PENDING)
                .requestType(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH)
                .subsetEuRequested("SubsetEuRequested")
                .subsetMsRequested("SubsetMsRequested")
                .eftiGateUrl("france")
                .fromGateUrl("https://efti.gate.france.eu")
                .transportMetaData(SearchParameter.builder()
                        .vehicleId("AA123VV")
                        .transportMode("ROAD")
                        .vehicleCountry("FR")
                        .isDangerousGoods(true)
                        .build())
                .build();
        when(controlRepository.save(any())).thenReturn(metadataControl);
        //Act
        final ControlDto cretaedControlDto = controlService.createControlFrom(messageBodyDto, "https://efti.gate.france.eu", metadataResultsDto);
        //Assert
        assertThat(cretaedControlDto)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expectedControl);
        verify(controlRepository, times(1)).save(any());
        verify(mapperUtils, times(1)).controlDtoToControEntity(any());
        verify(mapperUtils, times(1)).controlEntityToControlDto(any());
    }

    @Test
    void shouldGetMetadataResponse_whenControlExistsWithData() {
        //Arrange
        final ControlDto expectedControl = ControlDto.builder()
                .status(COMPLETE)
                .metadataResults(metadataResultsDto)
                .build();
        when(controlService.getControlByRequestUuid(requestUuid)).thenReturn(expectedControl);

        final MetadataResponseDto expectedMetadataResponse = MetadataResponseDto.builder()
                .status(COMPLETE)
                .metadata(List.of(metadataResultDto))
                .build();
        //Act
        final MetadataResponseDto metadataResponseDto = controlService.getMetadataResponse(requestUuid);

        //Assert
        assertThat(metadataResponseDto).isEqualTo(expectedMetadataResponse);
    }

    @Test
    void shouldGetMetadataResponseAsError_whenControlDoesNotExist() {
        //Arrange
        final ControlDto expectedControl = ControlDto.builder()
                .status(StatusEnum.ERROR)
                .error(ErrorDto.builder().errorCode(" Uuid not found.").errorDescription("Error requestUuid not found.").build())
                .build();
        when(controlService.getControlByRequestUuid(requestUuid)).thenReturn(expectedControl);
        final MetadataResponseDto expectedMetadataResponse = MetadataResponseDto.builder()
                .status(StatusEnum.ERROR)
                .errorDescription("Error requestUuid not found.")
                .errorCode(" Uuid not found.")
                .metadata(Collections.emptyList())
                .build();
        //Act
        final MetadataResponseDto metadataResponseDto = controlService.getMetadataResponse(requestUuid);

        //Assert
        assertThat(metadataResponseDto).isEqualTo(expectedMetadataResponse);
    }

    @Test
    void shouldGetControlWithCriteria_whenControlExistsAndIsUnique(){
        //Arrange
        final RequestEntity expectedRequest = new UilRequestEntity();
        uilRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        final ControlEntity expectedControl = ControlEntity.builder().requestUuid(requestUuid).status(StatusEnum.PENDING).requests(List.of(expectedRequest)).build();
        when(controlRepository.findByCriteria(anyString(), any(RequestStatusEnum.class))).thenReturn(List.of(expectedControl));
        //Act
        final ControlEntity control = controlService.getControlForCriteria(requestUuid, RequestStatusEnum.IN_PROGRESS);
        //Assert
        verify(controlRepository, times(1)).findByCriteria(requestUuid, RequestStatusEnum.IN_PROGRESS);
        assertThat(control).isEqualTo(expectedControl);
    }

    @Test
    void shouldThrowException_whenControlExistsAndIsNotUnique(){
        //Arrange
        final UilRequestEntity firstRequest = new UilRequestEntity();
        firstRequest.setStatus(RequestStatusEnum.IN_PROGRESS);
        final UilRequestEntity secondRequest = new UilRequestEntity();
        secondRequest.setStatus(RequestStatusEnum.IN_PROGRESS);
        final ControlEntity firstControl = ControlEntity.builder().requestUuid(requestUuid).status(StatusEnum.PENDING).requests(List.of(firstRequest)).build();
        final ControlEntity secondControl = ControlEntity.builder().requestUuid(requestUuid).status(StatusEnum.PENDING).requests(List.of(secondRequest)).build();

        when(controlRepository.findByCriteria(anyString(), any(RequestStatusEnum.class))).thenReturn(List.of(firstControl, secondControl));
        //Act && Assert
        final AmbiguousIdentifierException exception = assertThrows(AmbiguousIdentifierException.class,
                () -> controlService.getControlForCriteria(requestUuid, RequestStatusEnum.IN_PROGRESS));
        final String expectedMessage = String.format("Control with request uuid '%s', and request with status 'IN_PROGRESS' is not unique, 2 controls found!", requestUuid);
        final String actualMessage = exception.getMessage();

        verify(controlRepository, times(1)).findByCriteria(requestUuid, RequestStatusEnum.IN_PROGRESS);
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void shouldReturnNull_whenNoControlFound(){
        //Arrange
        when(controlRepository.findByCriteria(anyString(), any(RequestStatusEnum.class))).thenReturn(null);
        //Act
        final ControlEntity control = controlService.getControlForCriteria(requestUuid, RequestStatusEnum.IN_PROGRESS);
        //Assert
        verify(controlRepository, times(1)).findByCriteria(requestUuid, RequestStatusEnum.IN_PROGRESS);
        assertNull(control);
    }

    @Test
    void shouldNotUpdatePendingControl_whenControlRequestIsInProgessAndIsNotTimeout(){
        //Arrange
        identifiersRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        controlEntity.setRequests(List.of(identifiersRequestEntity));
        controlEntity.setStatus(StatusEnum.PENDING);
        controlEntity.setCreatedDate(LocalDateTime.now());

        //Act
        controlService.updatePendingControl(controlEntity);

        //Assert
        assertThat(controlEntity.getStatus()).isEqualTo(StatusEnum.PENDING);
    }

    @Test
    void shouldUpdatePendingControlToComplete_whenControlhasRequestsInSuccessAndRequestsHaveNoData(){
        //Arrange
        uilRequestEntity.setStatus(RequestStatusEnum.SUCCESS);
        controlEntity.setStatus(StatusEnum.PENDING);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);
        when(controlRepository.save(controlEntity)).thenReturn(controlEntity);

        //Act
        controlService.updatePendingControl(controlEntity);

        //Assert
        assertThat(controlEntity.getStatus()).isEqualTo(COMPLETE);
        assertNull(controlEntity.getEftiData());
    }

    @Test
    void shouldUpdatePendingControlToError_whenControlhasRequestInError(){
        //Arrange
        identifiersRequestEntity.setStatus(RequestStatusEnum.ERROR);
        controlEntity.setRequests(List.of(identifiersRequestEntity));
        controlEntity.setStatus(StatusEnum.PENDING);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);
        when(controlRepository.save(controlEntity)).thenReturn(controlEntity);

        //Act
        controlService.updatePendingControl(controlEntity);

        //Assert
        assertThat(controlEntity.getStatus()).isEqualTo(StatusEnum.ERROR);
    }

    @Test
    void shouldUpdatePendingControls(){
        //Arrange
        identifiersRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        controlEntity.setRequests(List.of(identifiersRequestEntity));
        controlEntity.setStatus(StatusEnum.PENDING);
        controlEntity.setCreatedDate(LocalDateTime.now().minusSeconds(100));

        when(controlRepository.findByCriteria(any(), anyInt())).thenReturn(List.of(controlEntity));
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(metadataRequestService);
        when(requestServiceFactory.getRequestServiceByRequestType(anyString())).thenReturn(metadataRequestService);
        when(controlRepository.save(any(ControlEntity.class))).thenReturn(controlEntity);

        //Act
        final int updatedControls = controlService.updatePendingControls();

        //Assert
        assertEquals(1, updatedControls);
    }

    @Test
    void shouldSaveControl(){
        //Arrange
        when(controlRepository.save(any())).thenReturn(controlEntity);

        //Act
        controlService.save(controlEntity);

        //Assert
        verify(controlRepository, times(1)).save(controlEntity);
    }

    @Test
    void shouldSaveControlDto(){
        //Arrange
        when(controlRepository.save(any())).thenReturn(controlEntity);

        //Act
        controlService.save(controlDto);

        //Assert
        verify(controlRepository, times(1)).save(controlEntity);
    }

    @Test
    void shouldNotSendRequestIfNotFoundOnLocalRegistry() {
        uilDto.setEFTIGateUrl("http://france.lol");

        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(metadataService.existByUIL(any(), any(), any())).thenReturn(false);

        final RequestUuidDto requestUuidDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        assertNotNull(requestUuidDtoResult);
        assertEquals(ErrorCodesEnum.DATA_NOT_FOUND_ON_REGISTRY.name(), requestUuidDtoResult.getErrorCode());
        assertEquals(ErrorCodesEnum.DATA_NOT_FOUND_ON_REGISTRY.getMessage(), requestUuidDtoResult.getErrorDescription());
    }

    @Test
    void shouldCreateNoteRequestForExistingControl() {
        notesDto.setRequestUuid("requestUuid");
        notesDto.setEFTIGateUrl("http://www.gate.com");
        notesDto.setEFTIDataUuid("12345678-ab12-4ab6-8999-123456789abc");
        notesDto.setEFTIPlatformUrl("http://www.platform.com");

        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(notesRequestService);
        when(controlRepository.findByRequestUuid(any())).thenReturn(Optional.of(controlEntity));


        final NoteResponseDto noteResponseDto = controlService.createNoteRequestForControl(notesDto);

        verify(notesRequestService, times(1)).createAndSendRequest(any(), any());
        verify(controlRepository, times(1)).save(any());
        assertNotNull(noteResponseDto);
        assertEquals("Note sent", noteResponseDto.getMessage());
        assertNull(noteResponseDto.getErrorCode());
        assertNull(noteResponseDto.getErrorDescription());
    }

    @Test
    void shouldNotCreateNoteRequestForNotControl() {
        notesDto.setRequestUuid("requestUuid");
        notesDto.setEFTIGateUrl("http://www.gate.com");
        notesDto.setEFTIDataUuid("12345678-ab12-4ab6-8999-123456789abc");
        notesDto.setEFTIPlatformUrl("http://www.platform.com");

        final NoteResponseDto noteResponseDto = controlService.createNoteRequestForControl(notesDto);

        verify(notesRequestService, never()).createAndSendRequest(any(), any());
        verify(controlRepository, never()).save(any());
        assertNotNull(noteResponseDto);
        assertEquals("note was not sent", noteResponseDto.getMessage());
        assertNotNull(noteResponseDto.getErrorCode());
        assertNotNull(noteResponseDto.getErrorDescription());
    }

    @Test
    void shouldGetCompleteAsControlNextStatus_whenAllRequestsAreInSuccessStatus() {
        identifiersRequestEntity.setStatus(RequestStatusEnum.SUCCESS);
        identifiersRequestEntity.setGateUrlDest("gate");

        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH);
        controlEntity.setRequests(List.of(identifiersRequestEntity));

        //Act
        final StatusEnum expectedControlStatus = controlService.getControlNextStatus(controlEntity);

        //assert

        assertEquals(COMPLETE, expectedControlStatus);
    }

    @Test
    void shouldGetErrorAsControlNextStatus_whenSomeRequestsAreInErrorStatus() {
        secondIdentifiersRequestEntity.setStatus(RequestStatusEnum.ERROR);
        identifiersRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        controlEntity.setRequests(List.of(identifiersRequestEntity, secondIdentifiersRequestEntity));

        //Act
        final StatusEnum expectedStatus = controlService.getControlNextStatus(controlEntity);

        //assert
        assertEquals(StatusEnum.ERROR, expectedStatus);
    }

    @Test
    void shouldGetTimeoutAsControlNextStatus_whenSomeRequestsAreInTimeoutStatus() {
        secondIdentifiersRequestEntity.setStatus(RequestStatusEnum.TIMEOUT);
        identifiersRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        controlEntity.setRequests(List.of(identifiersRequestEntity, secondIdentifiersRequestEntity));

        //Act
        final StatusEnum expectedStatus = controlService.getControlNextStatus(controlEntity);

        //assert
        assertEquals(StatusEnum.TIMEOUT, expectedStatus);
    }

    @Test
    void  shouldGetErrorAsControlNextStatusOverTimeout_whenAtLeastOneRequestIsInErrorStatus() {
        secondIdentifiersRequestEntity.setStatus(RequestStatusEnum.TIMEOUT);
        identifiersRequestEntity.setStatus(RequestStatusEnum.ERROR);
        controlEntity.setRequests(List.of(identifiersRequestEntity, secondIdentifiersRequestEntity));

        //Act
        final StatusEnum expectedStatus = controlService.getControlNextStatus(controlEntity);

        //assert
        assertEquals(StatusEnum.ERROR, expectedStatus);
    }
}