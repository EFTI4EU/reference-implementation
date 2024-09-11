package com.ingroupe.efti.eftigate.service.request;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ingroupe.common.test.log.MemoryAppender;
import com.ingroupe.efti.commons.dto.IdentifiersRequestDto;
import com.ingroupe.efti.commons.dto.MetadataDto;
import com.ingroupe.efti.commons.dto.MetadataResponseDto;
import com.ingroupe.efti.commons.dto.TransportVehicleDto;
import com.ingroupe.efti.commons.enums.CountryIndicator;
import com.ingroupe.efti.commons.enums.EDeliveryAction;
import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.enums.RequestTypeEnum;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationContentDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationType;
import com.ingroupe.efti.edeliveryapconnector.exception.SendRequestException;
import com.ingroupe.efti.eftigate.dto.RabbitRequestDto;
import com.ingroupe.efti.eftigate.entity.ControlEntity;
import com.ingroupe.efti.eftigate.entity.IdentifiersRequestEntity;
import com.ingroupe.efti.eftigate.entity.MetadataResult;
import com.ingroupe.efti.eftigate.entity.MetadataResults;
import com.ingroupe.efti.eftigate.exception.RequestNotFoundException;
import com.ingroupe.efti.eftigate.repository.IdentifiersRequestRepository;
import com.ingroupe.efti.eftigate.service.BaseServiceTest;
import com.ingroupe.efti.metadataregistry.entity.TransportVehicle;
import com.ingroupe.efti.metadataregistry.service.MetadataService;
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
import java.util.stream.Stream;

import static com.ingroupe.efti.commons.enums.RequestStatusEnum.IN_PROGRESS;
import static com.ingroupe.efti.commons.enums.RequestStatusEnum.RESPONSE_IN_PROGRESS;
import static com.ingroupe.efti.commons.enums.RequestStatusEnum.SUCCESS;
import static com.ingroupe.efti.commons.enums.StatusEnum.COMPLETE;
import static com.ingroupe.efti.eftigate.EftiTestUtils.testFile;
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
    @Mock
    private IdentifiersControlUpdateDelegateService identifiersControlUpdateDelegateService;
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
                        .vehicleID("abc123").countryStart("FR").vehicleCountry("FR").countryEnd("toto").build(), TransportVehicleDto.builder()
                        .vehicleID("abc124").countryStart("BE").vehicleCountry("BE").countryEnd("IT").build())).build();

        metadataResult = MetadataResult.builder()
                .eFTIDataUuid(DATA_UUID)
                .eFTIPlatformUrl(PLATFORM_URL)
                .transportVehicles(List.of(TransportVehicle.builder()
                        .vehicleID("abc123").vehicleCountry(CountryIndicator.FR).build(), TransportVehicle.builder()
                        .vehicleID("abc124").vehicleCountry(CountryIndicator.BE).build())).build();
        metadataRequestService = new MetadataRequestService(identifiersRequestRepository, mapperUtils, rabbitSenderService, controlService, gateProperties,
                metadataService, requestUpdaterService, serializeUtils, logManager, identifiersControlUpdateDelegateService);

        final Logger memoryAppenderTestLogger = (Logger) LoggerFactory.getLogger(MetadataRequestService.class);
        memoryAppender = MemoryAppender.createInitializedMemoryAppender(Level.INFO, memoryAppenderTestLogger);
    }

    @Test
    void shouldCreateAndSendRequest() {
        //Arrange
        when(identifiersRequestRepository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        //Act
        metadataRequestService.createAndSendRequest(controlDto, "https://efti.platform.borduria.eu");

        //Assert
        verify(mapperUtils, times(1)).requestDtoToRequestEntity(requestDtoArgumentCaptor.capture(), eq(IdentifiersRequestEntity.class));
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
        verify(controlService).createControlFrom(any(), any(), any());
        verify(identifiersRequestRepository, times(2)).save(any());
        verify(metadataService).search(any());
        verify(rabbitSenderService).sendMessageToRabbit(any(), any(), any());
    }

    @Test
    void shouldManageMessageReceiveAndUpdateExistingControlRequests() {
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .messageId(MESSAGE_ID)
                        .body(testFile("/xml/FTI021-full.xml"))
                        .fromPartyId("borduria.eu")
                        .build())
                .build();
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH);
        identifiersRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        controlEntity.setRequests(List.of(identifiersRequestEntity));
        when(controlService.existsByCriteria("67fe38bd-6bf7-4b06-b20e-206264bd639c")).thenReturn(true);

        //Act
        metadataRequestService.manageMessageReceive(notificationDto);

        //assert
        verify(identifiersControlUpdateDelegateService).updateExistingControl(anyString(), anyString(), anyString());
        verify(identifiersControlUpdateDelegateService).setControlNextStatus("67fe38bd-6bf7-4b06-b20e-206264bd639c");
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

        assertTrue(memoryAppender.containedInFormattedLogMessage("sent message messageId successfully"));
        assertEquals(1,memoryAppender.countEventsForLogger(MetadataRequestService.class.getName(), Level.INFO));
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
        final MetadataResponseDto metadataResponseDto = MetadataResponseDto.builder()
                .requestUuid(controlDto.getRequestUuid())
                .status(controlDto.getStatus())
                .metadata(List.of(metadataResultDto)).build();

        final String expectedRequestBody = testFile("/xml/FTI021.xml");

        when(controlService.buildMetadataResponse(any(), anyList())).thenReturn(metadataResponseDto);

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
