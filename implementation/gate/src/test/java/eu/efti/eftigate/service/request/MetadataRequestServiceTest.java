package eu.efti.eftigate.service.request;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import eu.efti.commons.dto.IdentifiersRequestDto;
import eu.efti.commons.dto.MetadataDto;
import eu.efti.commons.dto.MetadataResponseDto;
import eu.efti.commons.dto.TransportVehicleDto;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.commons.enums.EDeliveryAction;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.NotificationType;
import eu.efti.edeliveryapconnector.exception.SendRequestException;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.IdentifiersRequestEntity;
import eu.efti.eftigate.entity.MetadataResult;
import eu.efti.eftigate.entity.MetadataResults;
import eu.efti.eftigate.exception.RequestNotFoundException;
import eu.efti.eftigate.repository.IdentifiersRequestRepository;
import eu.efti.eftigate.service.BaseServiceTest;
import eu.efti.metadataregistry.entity.TransportVehicle;
import eu.efti.metadataregistry.service.MetadataService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.xmlunit.matchers.CompareMatcher;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static eu.efti.commons.enums.RequestStatusEnum.IN_PROGRESS;
import static eu.efti.commons.enums.RequestStatusEnum.RESPONSE_IN_PROGRESS;
import static eu.efti.commons.enums.RequestStatusEnum.SUCCESS;
import static eu.efti.commons.enums.StatusEnum.COMPLETE;
import static eu.efti.eftigate.EftiTestUtils.testFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetadataRequestServiceTest extends BaseServiceTest {
    private static final String DATA_UUID = "12345678-ab12-4ab6-8999-123456789abc";
    private static final String PLATFORM_URL = "http://efti.platform.truc.eu";

    @Mock
    private MetadataService metadataService;
    @Mock
    private IdentifiersRequestRepository identifiersRequestRepository;
    private MetadataRequestService metadataRequestService;
    @Captor
    private ArgumentCaptor<IdentifiersRequestDto> requestDtoArgumentCaptor;
    @Captor
    private ArgumentCaptor<IdentifiersRequestEntity> requestEntityArgumentCaptor;
    @Captor
    private ArgumentCaptor<ControlEntity> controlEntityArgumentCaptor;
    private MetadataDto metadataDto;
    private MetadataResult metadataResult;
    private final IdentifiersRequestEntity identifiersRequestEntity = new IdentifiersRequestEntity();
    private final IdentifiersRequestEntity secondIdentifiersRequestEntity = new IdentifiersRequestEntity();
    private final IdentifiersRequestDto identifiersRequestDto = new IdentifiersRequestDto();



    @Override
    @BeforeEach
    public void before() {
        super.before();
        super.setDtoRequestCommonAttributes(identifiersRequestDto);
        super.setEntityRequestCommonAttributes(identifiersRequestEntity);
        super.setEntityRequestCommonAttributes(secondIdentifiersRequestEntity);
        controlEntity.setRequests(List.of(identifiersRequestEntity, secondIdentifiersRequestEntity));

        metadataDto = MetadataDto.builder()
                .eFTIDataUuid(DATA_UUID)
                .eFTIPlatformUrl(PLATFORM_URL)
                .transportVehicles(List.of(TransportVehicleDto.builder()
                        .vehicleId("abc123").countryStart("FR").vehicleCountry("FR").countryEnd("toto").build(), TransportVehicleDto.builder()
                        .vehicleId("abc124").countryStart("BE").vehicleCountry("BE").countryEnd("IT").build())).build();

        metadataResult = MetadataResult.builder()
                .eFTIDataUuid(DATA_UUID)
                .eFTIPlatformUrl(PLATFORM_URL)
                .transportVehicles(List.of(TransportVehicle.builder()
                        .vehicleId("abc123").vehicleCountry(CountryIndicator.FR).build(), TransportVehicle.builder()
                        .vehicleId("abc124").vehicleCountry(CountryIndicator.BE).build())).build();
        metadataRequestService = new MetadataRequestService(identifiersRequestRepository, mapperUtils, rabbitSenderService, controlService, gateProperties, metadataService, requestUpdaterService, serializeUtils, logManager);

        final Logger memoryAppenderTestLogger = (Logger) LoggerFactory.getLogger(MetadataRequestService.class);
    }

    @Test
    void shouldCreateAndSendRequest() {
        //Arrange
        when(identifiersRequestRepository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        //Act
        metadataRequestService.createAndSendRequest(controlDto, "https://efti.platform.borduria.eu");

        //Assert
        verify(mapperUtils, times(2)).requestDtoToRequestEntity(requestDtoArgumentCaptor.capture(), eq(IdentifiersRequestEntity.class));
        assertEquals("https://efti.platform.borduria.eu", requestDtoArgumentCaptor.getValue().getGateUrlDest());
    }

    @Test
    void shouldCreateRequest() {
        //Arrange
        when(identifiersRequestRepository.save(any())).thenReturn(identifiersRequestEntity);

        //Act
        metadataRequestService.createRequest(controlDto, SUCCESS, Collections.singletonList(metadataDto));

        //Assert
        verify(mapperUtils).requestDtoToRequestEntity(requestDtoArgumentCaptor.capture(), eq(IdentifiersRequestEntity.class));
        assertEquals(metadataResult.getMetadataUUID(), requestDtoArgumentCaptor.getValue().getMetadataResults().getMetadataResult().get(0).getMetadataUUID());
        assertEquals(metadataResult.getEFTIDataUuid(), requestDtoArgumentCaptor.getValue().getMetadataResults().getMetadataResult().get(0).getEFTIDataUuid());
        assertEquals(metadataResult.getEFTIPlatformUrl(), requestDtoArgumentCaptor.getValue().getMetadataResults().getMetadataResult().get(0).getEFTIPlatformUrl());
        assertEquals(metadataResult.getTransportVehicles().size(), requestDtoArgumentCaptor.getValue().getMetadataResults().getMetadataResult().get(0).getTransportVehicles().size());
        assertEquals(SUCCESS, requestDtoArgumentCaptor.getValue().getStatus());
    }
    
    @Test
    void trySendDomibusSuccessTest() throws SendRequestException, JsonProcessingException {
        when(identifiersRequestRepository.save(any())).thenReturn(identifiersRequestEntity);

        metadataRequestService.sendRequest(requestDto);
        verify(rabbitSenderService).sendMessageToRabbit(any(), any(), any());
    }

    @Test
    void shouldManageMessageReceiveAndCreateNewControl_whenControlDoesNotExist() throws IOException {
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .messageId(MESSAGE_ID)
                        .body(testFile("/xml/FTI019.xml"))
                        .build())
                .build();
        when(controlService.getControlByRequestUuid(anyString())).thenReturn(controlDto);
        when(controlService.createControlFrom(any(), any(), any())).thenReturn(controlDto);
        when(identifiersRequestRepository.save(any())).thenReturn(identifiersRequestEntity);
        //Act
        metadataRequestService.manageMessageReceive(notificationDto);

        //assert
        verify(controlService).getControlForCriteria("67fe38bd-6bf7-4b06-b20e-206264bd639c", RequestStatusEnum.IN_PROGRESS);
        verify(controlService).createControlFrom(any(), any(), any());
        verify(identifiersRequestRepository, times(2)).save(any());
        verify(metadataService).search(any());
        verify(rabbitSenderService).sendMessageToRabbit(any(), any(), any());
    }

    @Test
    void shouldManageMessageReceiveAndUpdateExistingControlStatusAsComplete() throws IOException {
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .messageId(MESSAGE_ID)
                        .body(testFile("/xml/FTI021-full.xml"))
                        .fromPartyId("gate")
                        .build())
                .build();
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH);
        identifiersRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        identifiersRequestEntity.setGateUrlDest("gate");

        controlEntity.setRequests(List.of(identifiersRequestEntity));
        when(controlService.getControlForCriteria("67fe38bd-6bf7-4b06-b20e-206264bd639c", RequestStatusEnum.IN_PROGRESS)).thenReturn(controlEntity);
        when(mapperUtils.metadataResultDtosToMetadataEntities(anyList())).thenReturn(List.of(metadataResult));

        //Act
        metadataRequestService.manageMessageReceive(notificationDto);

        //assert
        verify(controlService, never()).createControlFrom(any(), any(), any());
        verify(metadataService, never()).search(any());
        verify(rabbitSenderService, never()).sendMessageToRabbit(any(), any(), any());

        verify(controlService).save(controlEntityArgumentCaptor.capture());
        assertEquals(COMPLETE, controlEntityArgumentCaptor.getValue().getStatus());
        assertEquals(SUCCESS, controlEntityArgumentCaptor.getValue().getRequests().iterator().next().getStatus());
        assertFalse(controlEntityArgumentCaptor.getValue().getMetadataResults().getMetadataResult().isEmpty());
        final IdentifiersRequestEntity identifiersRequest = (IdentifiersRequestEntity) controlEntityArgumentCaptor.getValue().getRequests().iterator().next();
        assertFalse(identifiersRequest.getMetadataResults().getMetadataResult().isEmpty());
    }

    @Test
    void shouldManageMessageReceiveAndUpdateExistingControlStatusAsError_whenSomeRequestsAreInErrorStatus() throws IOException {
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .messageId(MESSAGE_ID)
                        .body(testFile("/xml/FTI021-full.xml"))
                        .fromPartyId("gate")
                        .build())
                .build();
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH);
        secondIdentifiersRequestEntity.setStatus(RequestStatusEnum.ERROR);
        identifiersRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        identifiersRequestEntity.setGateUrlDest("gate");
        controlEntity.setRequests(List.of(identifiersRequestEntity, secondIdentifiersRequestEntity));
        when(controlService.getControlForCriteria("67fe38bd-6bf7-4b06-b20e-206264bd639c", RequestStatusEnum.IN_PROGRESS)).thenReturn(controlEntity);
        when(mapperUtils.metadataResultDtosToMetadataEntities(anyList())).thenReturn(List.of(metadataResult));

        //Act
        metadataRequestService.manageMessageReceive(notificationDto);

        //assert
        verify(controlService, never()).createControlFrom(any(), any(), any());
        verify(metadataService, never()).search(any());
        verify(rabbitSenderService, never()).sendMessageToRabbit(any(), any(), any());

        verify(controlService).save(controlEntityArgumentCaptor.capture());
        assertEquals(StatusEnum.ERROR, controlEntityArgumentCaptor.getValue().getStatus());
        assertFalse(controlEntityArgumentCaptor.getValue().getMetadataResults().getMetadataResult().isEmpty());
        final IdentifiersRequestEntity identifiersRequest = (IdentifiersRequestEntity) controlEntityArgumentCaptor.getValue().getRequests().iterator().next();
        assertFalse(identifiersRequest.getMetadataResults().getMetadataResult().isEmpty());
    }

    @Test
    void shouldManageMessageReceiveAndUpdateExistingControlStatusAsTimeout_whenSomeRequestsAreInTimeoutStatus() throws IOException {
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .messageId(MESSAGE_ID)
                        .body(testFile("/xml/FTI021-full.xml"))
                        .fromPartyId("gate")
                        .build())
                .build();
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH);
        secondIdentifiersRequestEntity.setStatus(RequestStatusEnum.TIMEOUT);
        identifiersRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        identifiersRequestEntity.setGateUrlDest("gate");
        controlEntity.setRequests(List.of(identifiersRequestEntity, secondIdentifiersRequestEntity));
        when(controlService.getControlForCriteria("67fe38bd-6bf7-4b06-b20e-206264bd639c", RequestStatusEnum.IN_PROGRESS)).thenReturn(controlEntity);
        when(mapperUtils.metadataResultDtosToMetadataEntities(anyList())).thenReturn(List.of(metadataResult));

        //Act
        metadataRequestService.manageMessageReceive(notificationDto);

        //assert
        verify(controlService, never()).createControlFrom(any(), any(), any());
        verify(metadataService, never()).search(any());
        verify(rabbitSenderService, never()).sendMessageToRabbit(any(), any(), any());

        verify(controlService).save(controlEntityArgumentCaptor.capture());
        assertEquals(StatusEnum.TIMEOUT, controlEntityArgumentCaptor.getValue().getStatus());
        assertFalse(controlEntityArgumentCaptor.getValue().getMetadataResults().getMetadataResult().isEmpty());
        final IdentifiersRequestEntity identifiersRequest = (IdentifiersRequestEntity) controlEntityArgumentCaptor.getValue().getRequests().iterator().next();
        assertFalse(identifiersRequest.getMetadataResults().getMetadataResult().isEmpty());
    }

    @Test
    void shouldManageMessageReceiveAndUpdateExistingControlStatusAseRROR_whenOneRequestsIsInErrorStatus() throws IOException {
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .messageId(MESSAGE_ID)
                        .body(testFile("/xml/FTI021-full.xml"))
                        .fromPartyId("gate")
                        .build())
                .build();
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH);
        secondIdentifiersRequestEntity.setStatus(RequestStatusEnum.TIMEOUT);
        identifiersRequestEntity.setStatus(RequestStatusEnum.ERROR);
        controlEntity.setRequests(List.of(identifiersRequestEntity, secondIdentifiersRequestEntity));
        when(controlService.getControlForCriteria("67fe38bd-6bf7-4b06-b20e-206264bd639c", RequestStatusEnum.IN_PROGRESS)).thenReturn(controlEntity);
        when(mapperUtils.metadataResultDtosToMetadataEntities(anyList())).thenReturn(List.of(metadataResult));

        //Act
        metadataRequestService.manageMessageReceive(notificationDto);

        //assert
        verify(controlService, never()).createControlFrom(any(), any(), any());
        verify(metadataService, never()).search(any());
        verify(rabbitSenderService, never()).sendMessageToRabbit(any(), any(), any());

        verify(controlService).save(controlEntityArgumentCaptor.capture());
        assertEquals(StatusEnum.ERROR, controlEntityArgumentCaptor.getValue().getStatus());
        assertFalse(controlEntityArgumentCaptor.getValue().getMetadataResults().getMetadataResult().isEmpty());
    }

    @Test
    void shouldManageMessageReceiveAndUpdateExistingControlMetadatas() {
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .messageId(MESSAGE_ID)
                        .body(testFile("/xml/FTI021-full.xml"))
                        .build())
                .build();
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH);
        identifiersRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        identifiersRequestEntity.setMetadataResults(metadataResults);
        controlEntity.setRequests(List.of(identifiersRequestEntity));
        controlEntity.setMetadataResults(metadataResults);

        when(controlService.getControlForCriteria("67fe38bd-6bf7-4b06-b20e-206264bd639c", RequestStatusEnum.IN_PROGRESS)).thenReturn(controlEntity);
        when(mapperUtils.metadataResultDtosToMetadataEntities(anyList())).thenReturn(List.of(metadataResult));

        //Act
        metadataRequestService.manageMessageReceive(notificationDto);

        //assert
        verify(controlService).save(controlEntityArgumentCaptor.capture());
        assertEquals(2, controlEntityArgumentCaptor.getValue().getMetadataResults().getMetadataResult().size());
        final IdentifiersRequestEntity identifiersRequest = (IdentifiersRequestEntity) controlEntityArgumentCaptor.getValue().getRequests().iterator().next();
        assertEquals(1, identifiersRequest.getMetadataResults().getMetadataResult().size());
    }

    @Test
    void allRequestsContainsDataTest_whenFalse() {
        assertFalse(metadataRequestService.allRequestsContainsData(List.of(identifiersRequestEntity)));
    }

    @Test
    void allRequestsContainsDataTest_whenTrue() {
        //Arrange
        identifiersRequestEntity.setMetadataResults(new MetadataResults(List.of(metadataResult)));
        //Act and Assert
        assertTrue(metadataRequestService.allRequestsContainsData(List.of(identifiersRequestEntity)));
    }

    @Test
    void getDataFromRequestsTest() {
        //Arrange
        final MetadataResult metadataResult1 = new MetadataResult();
        metadataResult1.setCountryStart("FR");
        metadataResult1.setCountryEnd("FR");
        metadataResult1.setDisabled(false);
        metadataResult1.setDangerousGoods(true);

        final MetadataResults metadataResults1 = new MetadataResults();
        metadataResults1.setMetadataResult(List.of(metadataResult1));

        final MetadataResult metadataResult2 = new MetadataResult();
        metadataResult2.setCountryStart("FR");
        metadataResult2.setCountryEnd("FR");
        metadataResult2.setDisabled(false);
        metadataResult2.setDangerousGoods(true);

        final MetadataResults metadataResults2 = new MetadataResults();
        metadataResults2.setMetadataResult(List.of(metadataResult2));

        identifiersRequestEntity.setMetadataResults(metadataResults1);
        secondIdentifiersRequestEntity.setMetadataResults(metadataResults2);

        final ControlEntity controlEntity = ControlEntity.builder().requests(List.of(identifiersRequestEntity, secondIdentifiersRequestEntity)).build();
        //Act
        metadataRequestService.setDataFromRequests(controlEntity);

        //Assert
        assertNotNull(controlEntity.getMetadataResults());
        assertEquals(2, controlEntity.getMetadataResults().getMetadataResult().size());
    }

    @Test
    void shouldUpdateControlAndRequestStatus_whenResponseSentSuccessfullyForExternalRequest() {
        identifiersRequestEntity.setEdeliveryMessageId(MESSAGE_ID);
        when(identifiersRequestRepository.findByControlRequestTypeAndStatusAndEdeliveryMessageId(any(), any(), any())).thenReturn(identifiersRequestEntity);

        metadataRequestService.manageSendSuccess(MESSAGE_ID);

        verify(identifiersRequestRepository).save(requestEntityArgumentCaptor.capture());
        assertEquals(COMPLETE, requestEntityArgumentCaptor.getValue().getControl().getStatus());
        assertEquals(SUCCESS, requestEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void shouldNotUpdateControlAndRequestStatus_AndLogMessage_whenResponseSentSuccessfully() {
        identifiersRequestEntity.setEdeliveryMessageId(MESSAGE_ID);
        metadataRequestService.manageSendSuccess(MESSAGE_ID);

    }

    @Test
    void shouldUpdateSentRequestStatus_whenRequestIsExternal(){
        identifiersRequestDto.getControl().setRequestType(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH);
        when(mapperUtils.requestToRequestDto(identifiersRequestEntity, IdentifiersRequestDto.class)).thenReturn(identifiersRequestDto);
        when(mapperUtils.requestDtoToRequestEntity(identifiersRequestDto, IdentifiersRequestEntity.class)).thenReturn(identifiersRequestEntity);
        when(identifiersRequestRepository.save(any())).thenReturn(identifiersRequestEntity);

        metadataRequestService.updateSentRequestStatus(identifiersRequestDto, MESSAGE_ID);

        verify(mapperUtils, times(1)).requestDtoToRequestEntity(requestDtoArgumentCaptor.capture(), eq(IdentifiersRequestEntity.class));
        assertEquals(RESPONSE_IN_PROGRESS, identifiersRequestDto.getStatus());
    }

    @Test
    void shouldUpdateSentRequestStatus_whenRequestIsNotExternal(){
        identifiersRequestDto.getControl().setRequestType(RequestTypeEnum.EXTERNAL_METADATA_SEARCH);
        when(mapperUtils.requestToRequestDto(identifiersRequestEntity, IdentifiersRequestDto.class)).thenReturn(identifiersRequestDto);
        when(mapperUtils.requestDtoToRequestEntity(identifiersRequestDto, IdentifiersRequestEntity.class)).thenReturn(identifiersRequestEntity);
        when(identifiersRequestRepository.save(any())).thenReturn(identifiersRequestEntity);

        metadataRequestService.updateSentRequestStatus(identifiersRequestDto, MESSAGE_ID);

        verify(mapperUtils, times(1)).requestDtoToRequestEntity(requestDtoArgumentCaptor.capture(), eq(IdentifiersRequestEntity.class));
        assertEquals(IN_PROGRESS, identifiersRequestDto.getStatus());
    }

    @Test
    void shouldBuildRequestBody_whenRemoteGateSentResponse(){
        controlDto.setRequestType(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH);
        controlDto.setMetadataResults(metadataResultsDto);
        final RabbitRequestDto rabbitRequestDto = new RabbitRequestDto();
        rabbitRequestDto.setControl(controlDto);
        final MetadataResponseDto metadataResponseDto = eu.efti.commons.dto.MetadataResponseDto.builder()
                .requestUuid(controlDto.getRequestUuid())
                .status(controlDto.getStatus())
                .metadata(List.of(metadataResultDto)).build();

        final String expectedRequestBody = testFile("/xml/FTI021.xml");

        when(controlService.buildMetadataResponse(any())).thenReturn(metadataResponseDto);

        final String requestBody = metadataRequestService.buildRequestBody(rabbitRequestDto);

        assertThat(StringUtils.deleteWhitespace(expectedRequestBody), CompareMatcher.isIdenticalTo(requestBody));
    }

    @Test
    void shouldBuildRequestBody_whenLocalGateSendsRequest(){
        controlDto.setRequestType(RequestTypeEnum.EXTERNAL_METADATA_SEARCH);
        controlDto.setMetadataResults(metadataResultsDto);
        controlDto.setTransportMetaData(searchParameter);
        final RabbitRequestDto rabbitRequestDto = new RabbitRequestDto();
        rabbitRequestDto.setControl(controlDto);
        final String expectedRequestBody = testFile("/xml/FTI013.xml");

        final String requestBody = metadataRequestService.buildRequestBody(rabbitRequestDto);

        assertThat(StringUtils.deleteWhitespace(expectedRequestBody), CompareMatcher.isIdenticalTo(requestBody));
    }

    @Test
    void shouldFindRequestByMessageId_whenRequestExists(){
        when(identifiersRequestRepository.findByEdeliveryMessageId(anyString())).thenReturn(identifiersRequestEntity);
        final IdentifiersRequestEntity requestByMessageId = metadataRequestService.findRequestByMessageIdOrThrow(MESSAGE_ID);
        assertNotNull(requestByMessageId);
    }

    @Test
    void shouldThrowException_whenFindRequestByMessageId_andRequestDoesNotExists() {
        final Exception exception = assertThrows(RequestNotFoundException.class, () -> {
            metadataRequestService.findRequestByMessageIdOrThrow(MESSAGE_ID);
        });
        assertEquals("couldn't find Identifiers request for messageId: messageId", exception.getMessage());
    }

    @Test
    void shouldUpdateControlWithMetadatas_whenControlHasNotMetadatas() {
        controlEntity.setMetadataResults(null);
        when(controlService.getByRequestUuid(anyString())).thenReturn(Optional.of(controlEntity));
        when(mapperUtils.metadataDtosToMetadataEntities(anyList())).thenReturn(List.of(metadataResult));

        metadataRequestService.updateControlMetadata(controlDto, List.of(metadataDto));

        verify(controlService).save(controlEntityArgumentCaptor.capture());
        assertNotNull(controlEntityArgumentCaptor.getValue().getMetadataResults());
        assertFalse(controlEntityArgumentCaptor.getValue().getMetadataResults().getMetadataResult().isEmpty());
        assertEquals(1, controlEntityArgumentCaptor.getValue().getMetadataResults().getMetadataResult().size());
        assertEquals(List.of(metadataResult), controlEntityArgumentCaptor.getValue().getMetadataResults().getMetadataResult());
    }

    @Test
    void shouldUpdateControlWithMetadatas_whenControlHasMetadatas() {
        controlEntity.setMetadataResults(metadataResults);
        when(controlService.getByRequestUuid(anyString())).thenReturn(Optional.of(controlEntity));
        when(mapperUtils.metadataDtosToMetadataEntities(anyList())).thenReturn(List.of(metadataResult));

        metadataRequestService.updateControlMetadata(controlDto, List.of(metadataDto));

        verify(controlService).save(controlEntityArgumentCaptor.capture());
        assertNotNull(controlEntityArgumentCaptor.getValue().getMetadataResults());
        assertFalse(controlEntityArgumentCaptor.getValue().getMetadataResults().getMetadataResult().isEmpty());
        assertEquals(2, controlEntityArgumentCaptor.getValue().getMetadataResults().getMetadataResult().size());
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForEdeliveryActionSupport")
    void supports_ShouldReturnTrueForIdentifiers(final EDeliveryAction eDeliveryAction, final boolean expectedResult) {
        assertEquals(expectedResult, metadataRequestService.supports(eDeliveryAction));
    }

    private static Stream<Arguments> getArgumentsForEdeliveryActionSupport() {
        return Stream.of(
                Arguments.of(EDeliveryAction.GET_IDENTIFIERS, true),
                Arguments.of(EDeliveryAction.SEND_NOTES, false),
                Arguments.of(EDeliveryAction.GET_UIL, false),
                Arguments.of(EDeliveryAction.UPLOAD_METADATA, false),
                Arguments.of(EDeliveryAction.FORWARD_UIL, false)
        );
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForRequestTypeEnumSupport")
    void supports_ShouldReturnTrueForIdentifiers(final RequestTypeEnum requestTypeEnum, final boolean expectedResult) {
        assertEquals(expectedResult, metadataRequestService.supports(requestTypeEnum));
    }

    private static Stream<Arguments> getArgumentsForRequestTypeEnumSupport() {
        return Stream.of(
                Arguments.of(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH, true),
                Arguments.of(RequestTypeEnum.EXTERNAL_METADATA_SEARCH, true),
                Arguments.of(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH, false),
                Arguments.of(RequestTypeEnum.EXTERNAL_UIL_SEARCH, false),
                Arguments.of(RequestTypeEnum.EXTERNAL_NOTE_SEND, false),
                Arguments.of(RequestTypeEnum.LOCAL_METADATA_SEARCH, true),
                Arguments.of(RequestTypeEnum.LOCAL_UIL_SEARCH, false)
        );
    }

}
