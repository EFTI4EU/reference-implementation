package eu.efti.eftigate.service.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ErrorDto;
import eu.efti.commons.dto.UilRequestDto;
import eu.efti.commons.enums.EDeliveryAction;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.NotificationType;
import eu.efti.edeliveryapconnector.exception.SendRequestException;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.UilRequestEntity;
import eu.efti.eftigate.exception.RequestNotFoundException;
import eu.efti.eftigate.repository.UilRequestRepository;
import eu.efti.eftigate.service.BaseServiceTest;
import eu.efti.eftigate.EftiTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static eu.efti.commons.enums.RequestStatusEnum.ERROR;
import static eu.efti.commons.enums.RequestStatusEnum.SUCCESS;
import static eu.efti.commons.enums.StatusEnum.COMPLETE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

@ExtendWith(MockitoExtension.class)
class UilRequestServiceTest extends BaseServiceTest {
    private UilRequestService uilRequestService;
    @Mock
    private UilRequestRepository uilRequestRepository;
    @Captor
    ArgumentCaptor<UilRequestEntity> uilRequestEntityArgumentCaptor;

    private final UilRequestEntity uilRequestEntity = new UilRequestEntity();
    private final UilRequestEntity secondUilRequestEntity = new UilRequestEntity();

    @Override
    @BeforeEach
    public void before() {
        super.before();
        super.setEntityRequestCommonAttributes(uilRequestEntity);
        super.setEntityRequestCommonAttributes(secondUilRequestEntity);
        controlEntity.setRequests(List.of(uilRequestEntity, secondUilRequestEntity));
        uilRequestService = new UilRequestService(uilRequestRepository, mapperUtils, rabbitSenderService, controlService,
                gateProperties, requestUpdaterService, serializeUtils, logManager);
    }

    @Test
    void manageSendErrorTest() {
        final ErrorDto errorDto = ErrorDto.fromErrorCode(ErrorCodesEnum.AP_SUBMISSION_ERROR);
        final UilRequestDto requestDtoWithError = UilRequestDto.builder()
                .error(errorDto)
                .control(
                        ControlDto
                                .builder()
                                .error(errorDto)
                                .fromGateUrl("fromGateUrl")
                                .eftiGateUrl("eftiGateUrl")
                                .build()
                )
                .gateUrlDest("gateUrlDest")
                .requestType(RequestType.UIL)
                .build();
        final UilRequestEntity uilRequestEntityWithError = mapperUtils.requestDtoToRequestEntity(requestDtoWithError, UilRequestEntity.class);

        Mockito.when(uilRequestRepository.save(any())).thenReturn(uilRequestEntityWithError);

        uilRequestService.manageSendError(requestDtoWithError);

        verify(uilRequestRepository).save(uilRequestEntityArgumentCaptor.capture());
        assertEquals(ERROR, uilRequestEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void receiveGateRequestFromOtherGateSucessTest() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        final String messageId = "e94806cd-e52b-11ee-b7d3-0242ac120012@domibus.eu";
        final String content = """
              <uilResponse>
                  <consignment>
                  </consignment>
                  <status>COMPLETE</status>
                  <requestId>24414689-1abf-4a9f-b4df-de3a491a44c9</requestId>
              </uilResponse>
        """;

        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .action("forwardUil")
                        .body(content)
                        .contentType("application/json")
                        .fromPartyId("http://efti.gate.listenbourg.eu")
                        .messageId(messageId)
                        .build())
                .build();

        Mockito.when(uilRequestRepository.findByControlRequestUuidAndStatus(any(), any())).thenReturn(uilRequestEntity);
        Mockito.when(uilRequestRepository.save(any())).thenReturn(uilRequestEntity);

        uilRequestService.receiveGateRequest(notificationDto);

        verify(uilRequestRepository).save(uilRequestEntityArgumentCaptor.capture());
        verify(logManager).logReceivedMessage(any(), any(), any(), any());
        assertEquals(RequestStatusEnum.SUCCESS, uilRequestEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void receiveGateRequestFromOtherGateErrorNoDescriptionTest() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        final String messageId = "e94806cd-e52b-11ee-b7d3-0242ac120012@domibus.eu";
        final String content = """
          <uilResponse>
              <consignment>
              </consignment>
              <status>ERROR</status>
              <requestId>24414689-1abf-4a9f-b4df-de3a491a44c9</requestId>
          </uilResponse>
        """;
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .action("forwardUil")
                        .body(content)
                        .contentType("application/json")
                        .fromPartyId("http://efti.gate.listenbourg.eu")
                        .messageId(messageId)
                        .build())
                .build();

        Mockito.when(uilRequestRepository.findByControlRequestUuidAndStatus(any(), any())).thenReturn(uilRequestEntity);

        uilRequestService.receiveGateRequest(notificationDto);

        verify(logManager).logReceivedMessage(any(), any(), any(), any());
        verify(uilRequestRepository).save(uilRequestEntityArgumentCaptor.capture());
        assertEquals(RequestStatusEnum.ERROR, uilRequestEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void receiveGateRequestFromOtherGateErrorTest() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        final String messageId = "e94806cd-e52b-11ee-b7d3-0242ac120012@domibus.eu";
        final String content = """
          <uilResponse>
              <consignment>
              </consignment>
              <status>ERROR</status>
              <requestId>24414689-1abf-4a9f-b4df-de3a491a44c9</requestId>
          </uilResponse>
        """;
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .action("forwardUil")
                        .body(content)
                        .contentType("application/json")
                        .fromPartyId("http://efti.gate.listenbourg.eu")
                        .messageId(messageId)
                        .build())
                .build();

        Mockito.when(uilRequestRepository.findByControlRequestUuidAndStatus(any(), any())).thenReturn(uilRequestEntity);

        uilRequestService.receiveGateRequest(notificationDto);

        verify(logManager).logReceivedMessage(any(), any(), any(), any());
        verify(uilRequestRepository).save(uilRequestEntityArgumentCaptor.capture());
        assertEquals(RequestStatusEnum.ERROR, uilRequestEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void receiveGateRequestSuccessTest() {
        final String messageId = "e94806cd-e52b-11ee-b7d3-0242ac120012@domibus.eu";
        final String content = """
          <UILQuery>
              <requestId>24414689-1abf-4a9f-b4df-de3a491a44c9</requestId>
                <uil>
                  <gateId></gateId>
                  <platformId></platformId>
                  <datasetId></datasetId>
                </uil>
          </UILQuery>
        """;
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .action("forwardUil")
                        .body(content)
                        .contentType("application/json")
                        .fromPartyId("http://efti.gate.listenbourg.eu")
                        .messageId(messageId)
                        .build())
                .build();
        final ArgumentCaptor<ControlDto> argumentCaptorControlDto = ArgumentCaptor.forClass(ControlDto.class);

        uilRequestService.receiveGateRequest(notificationDto);

        verify(logManager).logReceivedMessage(any(), anyString(), anyString(), anyString());
        verify(controlService).createUilControl(argumentCaptorControlDto.capture());
        assertEquals(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH, argumentCaptorControlDto.getValue().getRequestType());
    }

    @Test
    void trySendDomibusSuccessTest() throws SendRequestException, JsonProcessingException {
        uilRequestService.sendRequest(requestDto);
        verify(rabbitSenderService).sendMessageToRabbit(any(), any(), any());
    }

    @Test
    void sendTest() throws JsonProcessingException {
        when(uilRequestRepository.save(any())).thenReturn(uilRequestEntity);

        uilRequestService.createAndSendRequest(controlDto, null);

        verify(uilRequestRepository, Mockito.times(1)).save(any());
        verify(rabbitSenderService, Mockito.times(1)).sendMessageToRabbit(any(), any(), any());

    }

    @Test
    void shouldUpdateResponseSucessFromPlatformAndShoulSendToGate() {
        final String messageId = "e94806cd-e52b-11ee-b7d3-0242ac120012@domibus.eu";
        final String eftiData = """
                  <body>
                    <requestUuid>test</requestUuid>
                    <status>COMPLETE</status>
                    <eFTIData><data>vive les datas</data></eFTIData>"
                  </body>
                  """;
        this.uilRequestEntity.getControl().setRequestType(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH);
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(eftiData)
                        .build())
                .build();
        final ArgumentCaptor<ControlEntity> controlEntityArgumentCaptor = ArgumentCaptor.forClass(ControlEntity.class);
        uilRequestEntity.getControl().setFromGateUrl("other");
        when(uilRequestRepository.findByControlRequestUuidAndStatus(any(), any())).thenReturn(uilRequestEntity);
        when(uilRequestRepository.save(any())).thenReturn(uilRequestEntity);

        uilRequestService.updateWithResponse(notificationDto);

        verify(controlService, times(2)).save(controlEntityArgumentCaptor.capture());
        assertEquals(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH, controlEntityArgumentCaptor.getValue().getRequestType());
    }

    @Test
    void shouldUpdateResponse() {
        final String messageId = "e94806cd-e52b-11ee-b7d3-0242ac120012@domibus.eu";
        final String eftiData = """
                  <body>
                    <requestUuid>test</requestUuid>
                    <status>COMPLETE</status>
                    <eFTIData><data>vive les datas</data></eFTIData>"
                  </body>
                  """;
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(eftiData)
                        .build())
                .build();
        when(uilRequestRepository.findByControlRequestUuidAndStatus(any(), any())).thenReturn(uilRequestEntity);
        when(uilRequestRepository.save(any())).thenReturn(uilRequestEntity);
        uilRequestService.updateWithResponse(notificationDto);

        verify(uilRequestRepository).save(uilRequestEntityArgumentCaptor.capture());
        assertNotNull(uilRequestEntityArgumentCaptor.getValue());
        assertEquals(RequestStatusEnum.SUCCESS, uilRequestEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void shouldUpdateErrorResponse() {
        final String messageId = "messageId";
        final String eftiData = """
                  <body>
                    <requestUuid>test</requestUuid>
                    <status>ERROR</status>
                    <eFTIData><data>vive les datas</data></eFTIData>"
                  </body>
                  """;
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(eftiData)
                        .build())
                .build();
        when(uilRequestRepository.findByControlRequestUuidAndStatus(any(), any())).thenReturn(uilRequestEntity);
        when(uilRequestRepository.save(any())).thenReturn(uilRequestEntity);
        uilRequestService.updateWithResponse(notificationDto);

        verify(uilRequestRepository, times(2)).save(uilRequestEntityArgumentCaptor.capture());
        assertNotNull(uilRequestEntityArgumentCaptor.getValue());
        assertEquals(RequestStatusEnum.ERROR, uilRequestEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void shouldUpdateStatus() {
        when(uilRequestRepository.save(any())).thenReturn(uilRequestEntity);
        uilRequestService.updateStatus(uilRequestEntity, ERROR, "12345");
        verify(uilRequestRepository).save(uilRequestEntityArgumentCaptor.capture());
        verify(uilRequestRepository,  Mockito.times(1)).save(any(UilRequestEntity.class));
        assertEquals(ERROR, uilRequestEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void shouldReThrowException() {
        final String messageId = "messageId";
        final String eftiData = """
                  <body>
                    <requestUuid>test</requestUuid>
                    <status>toto</status>
                  </body>
                  """;

        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(eftiData)
                        .build())
                .build();
        when(uilRequestRepository.findByControlRequestUuidAndStatus(any(), any())).thenReturn(null);

        assertThrows(RequestNotFoundException.class, () -> uilRequestService.updateWithResponse(notificationDto));

        verify(uilRequestRepository, never()).save(uilRequestEntityArgumentCaptor.capture());
    }

    @Test
    void allRequestsContainsDataTest_whenFalse() {
        //Act and Assert
        assertFalse(uilRequestService.allRequestsContainsData(List.of(uilRequestEntity)));
    }

    @Test
    void allRequestsContainsDataTest_whenTrue() {
        //Arrange
        final byte[] data = {10, 20, 30, 40};
        uilRequestEntity.setReponseData(data);
        //Act and Assert
        assertTrue(uilRequestService.allRequestsContainsData(List.of(uilRequestEntity)));
    }

    @Test
    void shouldUpdateControlAndRequestStatus_whenResponseSentSuccessfullyForExternalRequest() {
        uilRequestEntity.setEdeliveryMessageId(MESSAGE_ID);
        when(uilRequestRepository.findByControlRequestTypeAndStatusAndEdeliveryMessageId(any(), any(), any())).thenReturn(uilRequestEntity);

        uilRequestService.manageSendSuccess(MESSAGE_ID);

        verify(uilRequestRepository).save(uilRequestEntityArgumentCaptor.capture());
        assertEquals(COMPLETE, uilRequestEntityArgumentCaptor.getValue().getControl().getStatus());
        assertEquals(SUCCESS, uilRequestEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void shouldNotUpdateControlAndRequestStatus_AndLogMessage_whenResponseSentSuccessfully() {
        uilRequestEntity.setEdeliveryMessageId(MESSAGE_ID);
        uilRequestService.manageSendSuccess(MESSAGE_ID);
    }

    @Test
    void shouldBuildResponseBody_whenResponseInProgress(){
        controlDto.setRequestType(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH);
        final RabbitRequestDto rabbitRequestDto = new RabbitRequestDto();
        rabbitRequestDto.setControl(controlDto);
        rabbitRequestDto.setEFTIPlatformUrl("http://example.com");
        rabbitRequestDto.setStatus(RequestStatusEnum.RESPONSE_IN_PROGRESS);

        final String expectedRequestBody = EftiTestUtils.testFile("/xml/FTI022.xml");

        final String requestBody = uilRequestService.buildRequestBody(rabbitRequestDto);

        assertThat(expectedRequestBody, isIdenticalTo(requestBody).ignoreWhitespace());
    }

    @Test
    void shouldBuildRequestBody_whenReceived(){
        controlDto.setRequestType(RequestTypeEnum.EXTERNAL_UIL_SEARCH);
        final RabbitRequestDto rabbitRequestDto = new RabbitRequestDto();
        rabbitRequestDto.setControl(controlDto);
        rabbitRequestDto.setStatus(RequestStatusEnum.RECEIVED);

        final String expectedRequestBody = EftiTestUtils.testFile("/xml/FTI020.xml");

        final String requestBody = uilRequestService.buildRequestBody(rabbitRequestDto);

        assertThat(expectedRequestBody, isSimilarTo(requestBody).ignoreWhitespace());
    }

    @Test
    void shouldFindRequestByMessageId_whenRequestExists(){
        when(uilRequestRepository.findByEdeliveryMessageId(anyString())).thenReturn(uilRequestEntity);
        final UilRequestEntity requestByMessageId = uilRequestService.findRequestByMessageIdOrThrow(MESSAGE_ID);
        assertNotNull(requestByMessageId);
    }

    @Test
    void shouldThrowException_whenFindRequestByMessageId_andRequestDoesNotExists() {
        final Exception exception = assertThrows(RequestNotFoundException.class, () -> {
            uilRequestService.findRequestByMessageIdOrThrow(MESSAGE_ID);
        });
        assertEquals("couldn't find Uil request for messageId: messageId", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForEdeliveryActionSupport")
    void supports_ShouldReturnTrueForUil(final EDeliveryAction eDeliveryAction, final boolean expectedResult) {
        assertEquals(expectedResult, uilRequestService.supports(eDeliveryAction));
    }

    private static Stream<Arguments> getArgumentsForEdeliveryActionSupport() {
        return Stream.of(
                Arguments.of(EDeliveryAction.GET_IDENTIFIERS, false),
                Arguments.of(EDeliveryAction.SEND_NOTES, false),
                Arguments.of(EDeliveryAction.GET_UIL, true),
                Arguments.of(EDeliveryAction.UPLOAD_IDENTIFIERS, false),
                Arguments.of(EDeliveryAction.FORWARD_UIL, true)
        );
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForRequestTypeEnumSupport")
    void supports_ShouldReturnTrueForUil(final RequestTypeEnum requestTypeEnum, final boolean expectedResult) {
        assertEquals(expectedResult, uilRequestService.supports(requestTypeEnum));
    }

    private static Stream<Arguments> getArgumentsForRequestTypeEnumSupport() {
        return Stream.of(
                Arguments.of(RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH, false),
                Arguments.of(RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH, false),
                Arguments.of(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH, true),
                Arguments.of(RequestTypeEnum.EXTERNAL_UIL_SEARCH, true),
                Arguments.of(RequestTypeEnum.EXTERNAL_NOTE_SEND, false),
                Arguments.of(RequestTypeEnum.LOCAL_IDENTIFIERS_SEARCH, false),
                Arguments.of(RequestTypeEnum.LOCAL_UIL_SEARCH, true)
        );
    }
}
