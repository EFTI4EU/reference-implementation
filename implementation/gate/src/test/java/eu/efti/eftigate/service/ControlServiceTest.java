package eu.efti.eftigate.service;

import eu.efti.commons.dto.AuthorityDto;
import eu.efti.commons.dto.ContactInformationDto;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ErrorDto;
import eu.efti.commons.dto.IdentifiersResponseDto;
import eu.efti.commons.dto.IdentifiersResultsDto;
import eu.efti.commons.dto.PostFollowUpRequestDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.dto.SearchParameter;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.UilDto;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.dto.identifiers.api.ConsignmentApiDto;
import eu.efti.commons.dto.identifiers.api.IdentifierRequestResultDto;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.NoteResponseDto;
import eu.efti.eftigate.dto.RequestIdDto;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.IdentifiersRequestEntity;
import eu.efti.eftigate.entity.IdentifiersResults;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.entity.UilRequestEntity;
import eu.efti.eftigate.exception.AmbiguousIdentifierException;
import eu.efti.eftigate.repository.ControlRepository;
import eu.efti.eftigate.service.gate.EftiGateIdResolver;
import eu.efti.eftigate.service.request.IdentifiersRequestService;
import eu.efti.eftigate.service.request.NotesRequestService;
import eu.efti.eftigate.service.request.RequestServiceFactory;
import eu.efti.eftigate.service.request.UilRequestService;
import eu.efti.identifiersregistry.service.IdentifiersService;
import eu.efti.v1.edelivery.Identifier;
import eu.efti.v1.edelivery.IdentifierQuery;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private IdentifiersRequestService identifiersRequestService;
    @Mock
    private IdentifiersService identifiersService;

    private ControlService controlService;
    @Mock
    private EftiGateIdResolver eftiGateIdResolver;

    @Mock
    private LogManager logManager;

    @Mock
    private RequestServiceFactory requestServiceFactory;

    @Mock
    private EftiAsyncCallsProcessor eftiAsyncCallsProcessor;

    @Mock
    private Function<List<String>, RequestTypeEnum> gateToRequestTypeFunction;

    @Mock
    private SerializeUtils serializeUtils;

    @Captor
    private ArgumentCaptor<ControlEntity> controlEntityArgumentCaptor;

    private final UilDto uilDto = new UilDto();
    private final PostFollowUpRequestDto notesDto = new PostFollowUpRequestDto();
    private final SearchWithIdentifiersRequestDto searchWithIdentifiersRequestDto = new SearchWithIdentifiersRequestDto();
    private final ControlDto controlDto = new ControlDto();
    private final ControlEntity controlEntity = ControlEntity.builder().requestType(RequestTypeEnum.LOCAL_UIL_SEARCH).build();
    private final UilRequestEntity uilRequestEntity = new UilRequestEntity();
    private final IdentifiersRequestEntity identifiersRequestEntity = new IdentifiersRequestEntity();

    final ConsignmentDto identifiersResult = new ConsignmentDto();
    final IdentifiersResults identifiersResults = new IdentifiersResults();

    private final ConsignmentDto identifiersResultDto = new ConsignmentDto();
    private final IdentifiersResultsDto identifiersResultsDto = new IdentifiersResultsDto();

    private final ConsignmentApiDto identifiersApiResultDto = new ConsignmentApiDto();

    private final RequestIdDto requestIdDto = new RequestIdDto();
    private final String requestId = UUID.randomUUID().toString();

    private static final String URL = "http://france.lol";
    private static final String PASSWORD = "password";
    private static final String USERNAME = "username";

    @BeforeEach
    public void before() {
        final GateProperties gateProperties = GateProperties.builder()
                .owner("france")
                .country("FR")
                .ap(GateProperties.ApConfig.builder()
                        .url(URL)
                        .password(PASSWORD)
                        .username(USERNAME).build()).build();
        controlService = new ControlService(controlRepository, eftiGateIdResolver, identifiersService, mapperUtils,
                requestServiceFactory, logManager, gateToRequestTypeFunction, eftiAsyncCallsProcessor,
                gateProperties, serializeUtils);
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

        requestIdDto.setRequestId(requestId);
        requestIdDto.setStatus(status);

        this.uilDto.setGateId("france");
        this.uilDto.setDatasetId("12345678-ab12-4ab6-8999-123456789abc");
        this.uilDto.setPlatformId("ttf");

        this.searchWithIdentifiersRequestDto.setIdentifier("abc123");
        this.searchWithIdentifiersRequestDto.setRegistrationCountryCode("FR");
        this.searchWithIdentifiersRequestDto.setAuthority(authorityDto);
        this.searchWithIdentifiersRequestDto.setModeCode("1");

        this.controlDto.setEftiDataUuid(uilDto.getDatasetId());
        this.controlDto.setGateId(uilDto.getGateId());
        this.controlDto.setPlatformId(uilDto.getPlatformId());
        this.controlDto.setRequestId(requestId);
        this.controlDto.setRequestType(RequestTypeEnum.LOCAL_UIL_SEARCH);
        this.controlDto.setStatus(status);
        this.controlDto.setSubsetId("oki");
        this.controlDto.setCreatedDate(localDateTime);
        this.controlDto.setLastModifiedDate(localDateTime);
        this.controlDto.setAuthority(AuthorityDto.builder()
                .country("FR")
                .isEmergencyService(true)
                .legalContact(ContactInformationDto.builder().build())
                .workingContact(ContactInformationDto.builder().build())
                .nationalUniqueIdentifier("unique").build());

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


        identifiersResult.setDatasetId("datasetId");
        identifiersResult.setPlatformId("platformId");
        identifiersResult.setGateId("gateId");

        identifiersResults.setConsignments(Collections.singletonList(identifiersResult));

        uilRequestEntity.setControl(controlEntity);

        identifiersResults.setConsignments(Collections.singletonList(identifiersResult));

        identifiersResultDto.setDatasetId("datasetId");
        identifiersResultDto.setPlatformId("platformId");
        identifiersResultDto.setGateId("gateId");

        identifiersApiResultDto.setDatasetId("datasetId");
        identifiersApiResultDto.setPlatformId("platformId");
        identifiersApiResultDto.setGateId("gateId");

        identifiersResultsDto.setConsignments(Collections.singletonList(identifiersResultDto));

        setField(controlService, "timeoutValue", 60);
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
        uilDto.setGateId("france");

        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);
        when(identifiersService.findByUIL(any(), any(), any())).thenReturn(new ConsignmentDto());

        final RequestIdDto requestIdDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, times(1)).createAndSendRequest(any(), any());
        verify(controlRepository, times(1)).save(any());
        verify(logManager).logAppRequest(any(), any(), any(), any(), any());
        verify(logManager, never()).logAppResponse(any(), any(), any(), any(), any(), any(), any());
        assertNotNull(requestIdDtoResult);
        assertNull(requestIdDtoResult.getErrorCode());
        assertNull(requestIdDtoResult.getErrorDescription());
    }

    @Test
    void createControlEntityTest() {
        uilDto.setGateId("borduria");
        uilDto.setPlatformId("acme");
        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);

        final RequestIdDto requestIdDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, times(1)).createAndSendRequest(any(), any());
        verify(controlRepository, times(1)).save(any());
        verify(logManager).logAppRequest(any(), any(), any(), any(), any());
        verify(logManager, never()).logAppResponse(any(), any(), any(), any(), any(), any(), any());
        assertNotNull(requestIdDtoResult);
        assertNull(requestIdDtoResult.getErrorCode());
        assertNull(requestIdDtoResult.getErrorDescription());
    }

    @Test
    void createControlEntityErrorGateNullTest() {
        uilDto.setGateId(null);
        controlDto.setGateId(null);
        controlEntity.setStatus(StatusEnum.ERROR);

        final RequestIdDto requestIdDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(controlRepository, never()).save(any());
        verify(logManager).logAppResponse(any(), any(), any(), any(), any(), any(), any());
        assertNotNull(requestIdDtoResult);
        assertEquals(ErrorCodesEnum.UIL_GATE_MISSING.name(), requestIdDtoResult.getErrorCode());
        assertEquals("Missing parameter gateId", requestIdDtoResult.getErrorDescription());
    }

    @Test
    void createControlEntityErrorGateFormatTest() {
        uilDto.setGateId("http://france.com");
        controlEntity.setStatus(StatusEnum.ERROR);

        final RequestIdDto requestIdDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(controlRepository, times(0)).save(any());
        verify(logManager).logAppResponse(any(), any(), any(), any(), any(), any(), any());
        assertNotNull(requestIdDtoResult);
        assertEquals(ErrorCodesEnum.GATE_ID_INCORRECT_FORMAT.name(), requestIdDtoResult.getErrorCode());
        assertEquals("gateId format incorrect.", requestIdDtoResult.getErrorDescription());
    }

    @Test
    void createControlEntityErrorPlatformNullTest() {
        uilDto.setPlatformId(null);
        controlDto.setPlatformId(null);
        controlEntity.setStatus(StatusEnum.ERROR);

        final RequestIdDto requestIdDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(controlRepository, never()).save(any());
        verify(logManager).logAppResponse(any(), any(), any(), any(), any(), any(), any());
        assertNotNull(requestIdDtoResult);
        assertEquals(ErrorCodesEnum.UIL_PLATFORM_MISSING.name(), requestIdDtoResult.getErrorCode());
        assertEquals("Missing parameter platformId", requestIdDtoResult.getErrorDescription());
    }

    @Test
    void createControlEntityErrorPlatformFormatTest() {
        uilDto.setPlatformId("https://acme.com");
        controlEntity.setStatus(StatusEnum.ERROR);

        final RequestIdDto requestIdDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(controlRepository, times(0)).save(any());
        verify(logManager).logAppResponse(any(), any(), any(), any(), any(), any(), any());
        assertNotNull(requestIdDtoResult);
        assertEquals(ErrorCodesEnum.PLATFORM_ID_INCORRECT_FORMAT.name(), requestIdDtoResult.getErrorCode());
        assertEquals("platformId format incorrect.", requestIdDtoResult.getErrorDescription());
    }

    @Test
    void createControlEntityErrorUuidNullTest() {
        uilDto.setDatasetId(null);
        controlDto.setRequestId(null);
        controlEntity.setStatus(StatusEnum.ERROR);

        final RequestIdDto requestIdDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(controlRepository, never()).save(any());
        verify(logManager).logAppResponse(any(), any(), any(), any(), any(), any(), any());
        assertNotNull(requestIdDtoResult);
        assertEquals(ErrorCodesEnum.UIL_UUID_MISSING.name(), requestIdDtoResult.getErrorCode());
        assertEquals("Missing parameter datasetId", requestIdDtoResult.getErrorDescription());
    }

    @Test
    void createControlEntityErrorDatasetIdFormatTest() {
        uilDto.setDatasetId("toto");
        controlEntity.setStatus(StatusEnum.ERROR);

        final RequestIdDto requestIdDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(controlRepository, times(0)).save(any());
        verify(logManager).logAppResponse(any(), any(), any(), any(), any(), any(), any());
        assertNotNull(requestIdDtoResult);
        assertEquals(ErrorCodesEnum.DATASET_ID_INCORRECT_FORMAT.name(), requestIdDtoResult.getErrorCode());
        assertEquals("datasetId format is incorrect.", requestIdDtoResult.getErrorDescription());
    }

    @Test
    void getControlEntitySuccessTest() {
        uilRequestEntity.setStatus(RequestStatusEnum.SUCCESS);
        controlEntity.setStatus(StatusEnum.COMPLETE);
        controlEntity.setRequests(Collections.singletonList(uilRequestEntity));
        when(controlRepository.findByRequestId(any())).thenReturn(Optional.of(controlEntity));

        final RequestIdDto requestIdDtoResult = controlService.getControlEntity(requestId);

        verify(controlRepository, times(1)).findByRequestId(any());

        verify(logManager).logAppResponse(any(), any(), any(), any(), any(), any(), any());
        assertNotNull(requestIdDtoResult);
        assertEquals(requestIdDtoResult.getRequestId(), controlEntity.getRequestId());
    }

    @Test
    void getControlEntitySuccessTestWhenStatusComplete() {
        controlEntity.setStatus(StatusEnum.COMPLETE);
        when(controlRepository.findByRequestId(any())).thenReturn(Optional.of(controlEntity));

        final RequestIdDto requestIdDtoResult = controlService.getControlEntity(requestId);

        verify(controlRepository, times(1)).findByRequestId(any());

        verify(logManager).logAppResponse(any(), any(), any(), any(), any(), any(), any());
        assertNotNull(requestIdDtoResult);
        assertEquals(requestIdDtoResult.getRequestId(), controlEntity.getRequestId());
    }

    @Test
    void getControlEntityNotFoundTest() {
        when(controlRepository.findByRequestId(any())).thenReturn(Optional.empty());

        final RequestIdDto requestIdDtoResult = controlService.getControlEntity(requestId);

        verify(controlRepository, times(1)).findByRequestId(any());
        assertNotNull(requestIdDtoResult);
        assertEquals(StatusEnum.ERROR, requestIdDtoResult.getStatus());
        assertEquals(0, requestIdDtoResult.getData().length);
    }

    @Test
    void shouldSetError() {
        final String description = "description";
        final String code = "code";
        final ErrorDto errorDto = ErrorDto.builder()
                .errorDescription(description)
                .errorCode(code).build();
        when(controlRepository.save(any())).thenReturn(controlEntity);

        controlService.setError(controlDto, errorDto);

        verify(controlRepository).save(controlEntityArgumentCaptor.capture());
        assertEquals(StatusEnum.ERROR, controlEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void createIdentifiersControlTest() {
        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(identifiersRequestService);
        when(gateToRequestTypeFunction.apply(any())).thenReturn(RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH);
        when(eftiGateIdResolver.resolve(any(SearchWithIdentifiersRequestDto.class))).thenReturn(List.of("borduria"));


        final RequestIdDto requestIdDtoResult = controlService.createIdentifiersControl(searchWithIdentifiersRequestDto);
        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(identifiersRequestService, times(1)).createAndSendRequest(any(), any());
        verify(eftiAsyncCallsProcessor, never()).checkLocalRepoAsync(any(), any());
        verify(controlRepository, times(1)).save(any());
        assertNotNull(requestIdDtoResult);
        assertNull(requestIdDtoResult.getErrorCode());
        assertNull(requestIdDtoResult.getErrorDescription());
    }

    @Test
    void createIdentifiersControlForLocalRequestTest() {
        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(gateToRequestTypeFunction.apply(any())).thenReturn(RequestTypeEnum.LOCAL_IDENTIFIERS_SEARCH);
        when(eftiGateIdResolver.resolve(any(SearchWithIdentifiersRequestDto.class))).thenReturn(List.of("france"));


        final RequestIdDto requestIdDtoResult = controlService.createIdentifiersControl(searchWithIdentifiersRequestDto);
        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(identifiersRequestService, never()).createAndSendRequest(any(), any());
        verify(eftiAsyncCallsProcessor, times(1)).checkLocalRepoAsync(any(), any());
        verify(controlRepository, times(1)).save(any());
        assertNotNull(requestIdDtoResult);
        assertNull(requestIdDtoResult.getErrorCode());
        assertNull(requestIdDtoResult.getErrorDescription());
    }

    @Test
    void shouldCreateIdentifiersControlWithPendingStatus_whenSomeOfGivenDestinationGatesDoesNotExist() {
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH);
        searchWithIdentifiersRequestDto.setEftiGateIndicator(List.of("IT", "RO"));
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(identifiersRequestService);
        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(gateToRequestTypeFunction.apply(any())).thenReturn(RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH);
        when(eftiGateIdResolver.resolve(any(SearchWithIdentifiersRequestDto.class))).thenReturn(List.of("http://italie.it"));


        final RequestIdDto requestIdDtoResult = controlService.createIdentifiersControl(searchWithIdentifiersRequestDto);
        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(identifiersRequestService, times(1)).createAndSendRequest(any(), any());
        verify(eftiAsyncCallsProcessor, never()).checkLocalRepoAsync(any(), any());
        verify(controlRepository, times(1)).save(any());
        assertNotNull(requestIdDtoResult);
        assertEquals(StatusEnum.PENDING, requestIdDtoResult.getStatus());
    }

    @Test
    void shouldCreateIdentifiersControlWithPendingStatus_whenAllGivenDestinationGatesDoesNotExist() {
        searchWithIdentifiersRequestDto.setEftiGateIndicator(List.of("IT", "RO"));
        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(gateToRequestTypeFunction.apply(any())).thenReturn(RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH);


        final RequestIdDto requestIdDtoResult = controlService.createIdentifiersControl(searchWithIdentifiersRequestDto);
        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        verify(identifiersRequestService, never()).createAndSendRequest(any(), any());
        verify(eftiAsyncCallsProcessor, never()).checkLocalRepoAsync(any(), any());
        verify(controlRepository, times(1)).save(any());
        assertNotNull(requestIdDtoResult);
        assertEquals(StatusEnum.PENDING, requestIdDtoResult.getStatus());
    }

    @Test
    void createIdentifiersControlWithMinimumRequiredTest() {
        searchWithIdentifiersRequestDto.setModeCode(null);
        searchWithIdentifiersRequestDto.setRegistrationCountryCode(null);
        searchWithIdentifiersRequestDto.setEftiGateIndicator(null);
        searchWithIdentifiersRequestDto.setDangerousGoodsIndicator(null);

        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(gateToRequestTypeFunction.apply(any())).thenReturn(RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH);
        when(eftiGateIdResolver.resolve(any(SearchWithIdentifiersRequestDto.class))).thenReturn(List.of("borduria"));
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(identifiersRequestService);


        final RequestIdDto requestIdDtoResult = controlService.createIdentifiersControl(searchWithIdentifiersRequestDto);

        verify(identifiersRequestService, times(1)).createAndSendRequest(any(), any());
        verify(uilRequestService, times(0)).createAndSendRequest(any(), any());
        verify(controlRepository, times(1)).save(any());
        assertNotNull(requestIdDtoResult);
        assertNull(requestIdDtoResult.getErrorCode());
        assertNull(requestIdDtoResult.getErrorDescription());
    }

    @Test
    void createIdentifiersControlVehicleIDIncorrect() {
        searchWithIdentifiersRequestDto.setIdentifier("fausse plaque");

        final RequestIdDto requestIdDtoResult = controlService.createIdentifiersControl(searchWithIdentifiersRequestDto);

        verify(uilRequestService, times(0)).createAndSendRequest(any(), any());
        verify(controlRepository, times(0)).save(any());
        assertNotNull(requestIdDtoResult);
        assertEquals(ErrorCodesEnum.IDENTIFIER_INCORRECT_FORMAT.name(), requestIdDtoResult.getErrorCode());
        assertEquals(ErrorCodesEnum.IDENTIFIER_INCORRECT_FORMAT.getMessage(), requestIdDtoResult.getErrorDescription());
    }

    @Test
    void createIdentifiersControlVehicleCountryIncorrect() {
        searchWithIdentifiersRequestDto.setRegistrationCountryCode("toto");

        final RequestIdDto requestIdDtoResult = controlService.createIdentifiersControl(searchWithIdentifiersRequestDto);

        verify(uilRequestService, times(0)).createAndSendRequest(any(), any());
        verify(controlRepository, times(0)).save(any());
        assertNotNull(requestIdDtoResult);
        assertEquals(ErrorCodesEnum.REGISTRATION_COUNTRY_INCORRECT.name(), requestIdDtoResult.getErrorCode());
        assertEquals(ErrorCodesEnum.REGISTRATION_COUNTRY_INCORRECT.getMessage(), requestIdDtoResult.getErrorDescription());
    }

    @Test
    void createIdentifiersControlTransportModeIncorrect() {
        searchWithIdentifiersRequestDto.setModeCode("#toto");

        final RequestIdDto requestIdDtoResult = controlService.createIdentifiersControl(searchWithIdentifiersRequestDto);

        verify(uilRequestService, times(0)).createAndSendRequest(any(), any());
        verify(controlRepository, times(0)).save(any());
        assertNotNull(requestIdDtoResult);
        assertEquals(ErrorCodesEnum.MODE_CODE_INCORRECT_FORMAT.name(), requestIdDtoResult.getErrorCode());
        assertEquals(ErrorCodesEnum.MODE_CODE_INCORRECT_FORMAT.getMessage(), requestIdDtoResult.getErrorDescription());
    }

    @Test
    void shouldCreateControlFromNotificationDtoAndMessageBody() {
        //Arrange
        final ControlEntity identifiersControl = ControlEntity.builder()
                .id(1)
                .requestId("67fe38bd-6bf7-4b06-b20e-206264bd639c")
                .status(StatusEnum.PENDING)
                .requestType(RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH)
                .subsetId("full")
                .gateId("france")
                .fromGateId("https://efti.gate.france.eu")
                .transportIdentifiers(SearchParameter.builder()
                        .identifier("AA123VV")
                        .modeCode("1")
                        .registrationCountryCode("FR")
                        .dangerousGoodsIndicator(true)
                        .build())

                .build();
        final IdentifierQuery identifierQuery = new IdentifierQuery();
        Identifier identifier = new Identifier();
        identifier.setValue("AA123VV");
        identifierQuery.setIdentifier(identifier);
        identifierQuery.setRequestId("67fe38bd-6bf7-4b06-b20e-206264bd639c");
        identifierQuery.setRegistrationCountryCode("FR");
        identifierQuery.setModeCode("1");
        identifierQuery.setDangerousGoodsIndicator(true);

        final ControlDto expectedControl = ControlDto.builder()
                .requestId("67fe38bd-6bf7-4b06-b20e-206264bd639c")
                .status(StatusEnum.PENDING)
                .requestType(RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH)
                .subsetId("full")
                .gateId("france")
                .fromGateId("https://efti.gate.france.eu")
                .eftiData(new byte[0])
                .identifiersResults(List.of())
                .transportIdentifiers(SearchParameter.builder()
                        .identifier("AA123VV")
                        .modeCode("1")
                        .registrationCountryCode("FR")
                        .dangerousGoodsIndicator(true)
                        .build())
                .build();
        when(controlRepository.save(any())).thenReturn(identifiersControl);
        //Act
        final ControlDto createdControlDto = controlService.createControlFrom(identifierQuery, "https://efti.gate.france.eu");
        //Assert
        assertThat(createdControlDto)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expectedControl);
        verify(controlRepository, times(1)).save(any());
        verify(mapperUtils, times(1)).controlDtoToControlEntity(any());
        verify(mapperUtils, times(1)).controlEntityToControlDto(any());
    }

    @Test
    void shouldGetIdentifiersResponse_whenControlExistsWithData() {
        //Arrange
        final ControlDto expectedControl = ControlDto.builder()
                .status(StatusEnum.COMPLETE)
                .identifiersResults(identifiersResultsDto.getConsignments())
                .build();
        when(controlService.getControlByRequestId(requestId)).thenReturn(expectedControl);
        when(requestServiceFactory.getRequestServiceByRequestType(anyString())).thenReturn(identifiersRequestService);

        identifiersRequestEntity.setStatus(RequestStatusEnum.SUCCESS);
        identifiersRequestEntity.setIdentifiersResults(identifiersResults);
        when(identifiersRequestService.findAllForControlId(anyInt())).thenReturn(List.of(identifiersRequestEntity));

        final IdentifiersResponseDto expectedIdentifiersResponse = IdentifiersResponseDto.builder()
                .status(StatusEnum.COMPLETE)
                .identifiers(List.of(IdentifierRequestResultDto.builder()
                                .status(StatusEnum.COMPLETE.name())
                        .consignments(List.of(identifiersApiResultDto)).build()))
                .build();
        //Act
        final IdentifiersResponseDto identifiersResponseDto = controlService.getIdentifiersResponse(requestId);

        //Assert
        assertThat(identifiersResponseDto).isEqualTo(expectedIdentifiersResponse);
    }

    @Test
    void shouldGetIdentifiersResponseAsError_whenControlDoesNotExist() {
        //Arrange
        final ControlDto expectedControl = ControlDto.builder()
                .status(StatusEnum.ERROR)
                .error(ErrorDto.builder().errorCode(" Id not found.").errorDescription("Error requestId not found.").build())
                .build();
        when(controlService.getControlByRequestId(requestId)).thenReturn(expectedControl);
        when(requestServiceFactory.getRequestServiceByRequestType(anyString())).thenReturn(identifiersRequestService);

        final IdentifiersResponseDto expectedIdentifiersResponse = IdentifiersResponseDto.builder()
                .status(StatusEnum.ERROR)
                .errorDescription("Error requestId not found.")
                .errorCode(" Id not found.")
                .identifiers(Collections.emptyList())
                .build();
        //Act
        final IdentifiersResponseDto identifiersResponseDto = controlService.getIdentifiersResponse(requestId);

        //Assert
        assertThat(identifiersResponseDto).isEqualTo(expectedIdentifiersResponse);
    }

    @Test
    void shouldGetControlWithCriteria_whenControlExistsAndIsUnique() {
        //Arrange
        final RequestEntity expectedRequest = new UilRequestEntity();
        uilRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        final ControlEntity expectedControl = ControlEntity.builder().requestId(requestId).status(StatusEnum.PENDING).requests(List.of(expectedRequest)).build();
        when(controlRepository.findByCriteria(anyString(), any(RequestStatusEnum.class))).thenReturn(List.of(expectedControl));
        //Act
        final ControlEntity control = controlService.getControlForCriteria(requestId, RequestStatusEnum.IN_PROGRESS);
        //Assert
        verify(controlRepository, times(1)).findByCriteria(requestId, RequestStatusEnum.IN_PROGRESS);
        assertThat(control).isEqualTo(expectedControl);
    }

    @Test
    void shouldThrowException_whenControlExistsAndIsNotUnique() {
        //Arrange
        final UilRequestEntity firstRequest = new UilRequestEntity();
        firstRequest.setStatus(RequestStatusEnum.IN_PROGRESS);
        final UilRequestEntity secondRequest = new UilRequestEntity();
        secondRequest.setStatus(RequestStatusEnum.IN_PROGRESS);
        final ControlEntity firstControl = ControlEntity.builder().requestId(requestId).status(StatusEnum.PENDING).requests(List.of(firstRequest)).build();
        final ControlEntity secondControl = ControlEntity.builder().requestId(requestId).status(StatusEnum.PENDING).requests(List.of(secondRequest)).build();

        when(controlRepository.findByCriteria(anyString(), any(RequestStatusEnum.class))).thenReturn(List.of(firstControl, secondControl));
        //Act && Assert
        final AmbiguousIdentifierException exception = assertThrows(AmbiguousIdentifierException.class,
                () -> controlService.getControlForCriteria(requestId, RequestStatusEnum.IN_PROGRESS));
        final String expectedMessage = String.format("Control with request uuid '%s', and request with status 'IN_PROGRESS' is not unique, 2 controls found!", requestId);
        final String actualMessage = exception.getMessage();

        verify(controlRepository, times(1)).findByCriteria(requestId, RequestStatusEnum.IN_PROGRESS);
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void shouldReturnNull_whenNoControlFound() {
        //Arrange
        when(controlRepository.findByCriteria(anyString(), any(RequestStatusEnum.class))).thenReturn(null);
        //Act
        final ControlEntity control = controlService.getControlForCriteria(requestId, RequestStatusEnum.IN_PROGRESS);
        //Assert
        verify(controlRepository, times(1)).findByCriteria(requestId, RequestStatusEnum.IN_PROGRESS);
        assertNull(control);
    }

    @Test
    void shouldNotUpdatePendingControl_whenControlRequestIsInProgessAndIsNotTimeout() {
        //Arrange
        identifiersRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        controlEntity.setRequests(List.of(identifiersRequestEntity));
        controlEntity.setStatus(StatusEnum.PENDING);
        controlEntity.setCreatedDate(LocalDateTime.now());

        //Act
        ControlDto updatedControl = controlService.updatePendingControl(controlEntity);

        //Assert
        assertThat(updatedControl.getStatus()).isEqualTo(StatusEnum.PENDING);
    }

    @Test
    void shouldUpdatePendingControlToComplete_whenControlHasRequestsInSuccessWithoutData() {
        //Arrange
        uilRequestEntity.setStatus(RequestStatusEnum.SUCCESS);
        controlEntity.setStatus(StatusEnum.PENDING);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);
        when(controlRepository.save(controlEntity)).thenReturn(controlEntity);

        //Act
        ControlDto updatedControl = controlService.updatePendingControl(controlEntity);

        //Assert
        assertThat(updatedControl.getStatus()).isEqualTo(StatusEnum.COMPLETE);
    }

    @Test
    void shouldUpdatePendingControlToError_whenControlHasRequestInError() {
        //Arrange
        identifiersRequestEntity.setStatus(RequestStatusEnum.ERROR);
        controlEntity.setRequests(List.of(identifiersRequestEntity));
        controlEntity.setStatus(StatusEnum.PENDING);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);
        when(controlRepository.save(controlEntity)).thenReturn(controlEntity);

        //Act
        ControlDto updatedControl = controlService.updatePendingControl(controlEntity);

        //Assert
        assertThat(updatedControl.getStatus()).isEqualTo(StatusEnum.ERROR);
    }

    @Test
    void shouldUpdatePendingControlToTimeOut_whenSecSinceCreationExceedTimeoutValueAndRequestsAreInProgressWithoutData() {
        //Arrange
        identifiersRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        identifiersRequestEntity.setRequestType(RequestType.IDENTIFIER.name());
        controlEntity.setRequests(List.of(identifiersRequestEntity));
        controlEntity.setStatus(StatusEnum.PENDING);
        controlEntity.setCreatedDate(LocalDateTime.now().minusSeconds(100));

        when(controlRepository.findByCriteria(any(), anyInt())).thenReturn(List.of(controlEntity));
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(identifiersRequestService);
        when(requestServiceFactory.getRequestServiceByRequestType(anyString())).thenReturn(identifiersRequestService);
        when(controlRepository.save(any(ControlEntity.class))).thenReturn(controlEntity);

        //Act
        final int updatedControls = controlService.updatePendingControls();

        //Assert
        assertEquals(1, updatedControls);
        verify(controlRepository).save(controlEntityArgumentCaptor.capture());
        assertEquals(StatusEnum.TIMEOUT, controlEntityArgumentCaptor.getValue().getStatus());
        verify(identifiersRequestService, never()).notifyTimeout(any(RequestDto.class));
    }

    @Test
    void shouldUpdatePendingExternalControlToTimeOutAndNotifyIt_whenSecondsSinceCreationExceedTimeoutValueAndRequestsInProgressWithoutData() {
        //Arrange
        identifiersRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        identifiersRequestEntity.setRequestType(RequestType.IDENTIFIER.name());
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH);
        controlEntity.setRequests(List.of(identifiersRequestEntity));
        controlEntity.setStatus(StatusEnum.PENDING);
        controlEntity.setCreatedDate(LocalDateTime.now().minusSeconds(100));

        when(controlRepository.findByCriteria(any(), anyInt())).thenReturn(List.of(controlEntity));
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(identifiersRequestService);
        when(requestServiceFactory.getRequestServiceByRequestType(anyString())).thenReturn(identifiersRequestService);
        when(controlRepository.save(any(ControlEntity.class))).thenReturn(controlEntity);

        //Act
        final int updatedControls = controlService.updatePendingControls();

        //Assert
        assertEquals(1, updatedControls);
        verify(controlRepository).save(controlEntityArgumentCaptor.capture());
        assertEquals(StatusEnum.TIMEOUT, controlEntityArgumentCaptor.getValue().getStatus());
        verify(identifiersRequestService, times(1)).notifyTimeout(any(RequestDto.class));
    }

    @Test
    void shouldSaveControl() {
        //Arrange
        when(controlRepository.save(any())).thenReturn(controlEntity);

        //Act
        controlService.save(controlEntity);

        //Assert
        verify(controlRepository, times(1)).save(controlEntity);
    }

    @Test
    void shouldSaveControlDto() {
        //Arrange
        when(controlRepository.save(any())).thenReturn(controlEntity);

        //Act
        controlService.save(controlDto);

        //Assert
        verify(controlRepository, times(1)).save(controlEntity);
    }

    @Test
    void shouldNotSendRequestIfNotFoundOnLocalRegistry() {
        uilDto.setGateId("france");

        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(identifiersService.findByUIL(any(), any(), any())).thenReturn(null);

        final RequestIdDto requestIdDtoResult = controlService.createUilControl(uilDto);

        verify(uilRequestService, never()).createAndSendRequest(any(), any());
        assertNotNull(requestIdDtoResult);
        assertEquals(ErrorCodesEnum.DATA_NOT_FOUND_ON_REGISTRY.name(), requestIdDtoResult.getErrorCode());
        assertEquals(ErrorCodesEnum.DATA_NOT_FOUND_ON_REGISTRY.getMessage(), requestIdDtoResult.getErrorDescription());
    }

    @Test
    void shouldCreateNoteRequestForExistingControl() {
        notesDto.setRequestId("requestId");
        notesDto.setMessage("oki");
        controlEntity.setStatus(StatusEnum.COMPLETE);

        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(notesRequestService);
        when(controlRepository.findByRequestId(any())).thenReturn(Optional.of(controlEntity));


        final NoteResponseDto noteResponseDto = controlService.createNoteRequestForControl(notesDto);

        verify(notesRequestService, times(1)).createAndSendRequest(any(), any());
        assertNotNull(noteResponseDto);
        assertEquals("Note sent", noteResponseDto.getMessage());
        assertNull(noteResponseDto.getErrorCode());
        assertNull(noteResponseDto.getErrorDescription());
    }

    @Test
    void shouldNotCreateNoteRequest_whenAssociatedControlDoesNotExist() {
        notesDto.setRequestId("requestId");
        notesDto.setMessage("oki");

        final NoteResponseDto noteResponseDto = controlService.createNoteRequestForControl(notesDto);

        verify(notesRequestService, never()).createAndSendRequest(any(), any());
        verify(controlRepository, never()).save(any());
        assertNotNull(noteResponseDto);
        assertEquals("note was not sent", noteResponseDto.getMessage());
        assertNotNull(noteResponseDto.getErrorCode());
        assertNotNull(noteResponseDto.getErrorDescription());
    }

    @Test
    void shouldNotCreateNoteRequestForExistingControl_whenNoteHasMoreThan255Characters() {
        notesDto.setRequestId("requestId");
        notesDto.setMessage(RandomStringUtils.randomAlphabetic(256));
        controlEntity.setStatus(StatusEnum.COMPLETE);

        when(controlRepository.findByRequestId(any())).thenReturn(Optional.of(controlEntity));


        final NoteResponseDto noteResponseDto = controlService.createNoteRequestForControl(notesDto);

        assertNotNull(noteResponseDto);
        assertEquals("note was not sent", noteResponseDto.getMessage());
        assertEquals("NOTE_TOO_LONG", noteResponseDto.getErrorCode());
        assertEquals("Note max length is 255 characters.", noteResponseDto.getErrorDescription());
    }

    @Test
    void shouldCreateUilControl_whenGateIsNotTheCurrentOne() {
        //Arrange
        controlDto.setGateId("italy");
        controlDto.setRequestType(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH);
        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);

        //Act
        controlService.createUilControl(controlDto);

        //Assert
        verify(uilRequestService, times(1)).createAndSendRequest(any(), any());
    }

    @Test
    void shouldCreateUilControlAndRespondWithErrorForExternalAsk_whileOnCurrentGateAndDataNotFoundOnlocalRegistry() {
        //Arrange
        controlDto.setRequestType(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH);
        controlDto.setGateId("france");
        controlDto.setFromGateId("finland");
        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);

        //Act
        controlService.createUilControl(controlDto);

        //Assert
        verify(uilRequestService, times(1)).createAndSendRequest(any(), any(), any());
        verify(identifiersService, times(1)).findByUIL(any(), any(), any());
    }


    @Test
    void shouldFindControlByRequestId() {
        //Arrange
        when(controlRepository.findByRequestId(requestId)).thenReturn(Optional.of(controlEntity));

        //Act
        Optional<ControlEntity> result = controlService.findByRequestId(requestId);

        //Assert
        assertTrue(result.isPresent());
    }

    @Test
    void shouldGetByRequestId() {
        //Act
        controlService.getControlByRequestId(requestId);

        //Assert
        verify(controlRepository, times(1)).findByRequestId(requestId);
    }


}

