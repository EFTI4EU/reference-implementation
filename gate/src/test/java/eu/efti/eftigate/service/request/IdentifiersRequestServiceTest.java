package eu.efti.eftigate.service.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.efti.commons.dto.IdentifiersRequestDto;
import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.dto.identifiers.UsedTransportEquipmentDto;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.NotificationType;
import eu.efti.edeliveryapconnector.exception.SendRequestException;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.entity.IdentifiersRequestEntity;
import eu.efti.eftigate.entity.IdentifiersResults;
import eu.efti.eftigate.exception.RequestNotFoundException;
import eu.efti.eftigate.repository.IdentifiersRequestRepository;
import eu.efti.eftigate.service.BaseServiceTest;
import eu.efti.eftigate.service.IdentifiersControlUpdateDelegateService;
import eu.efti.eftigate.service.ValidationService;
import eu.efti.identifiersregistry.service.IdentifiersService;
import eu.efti.v1.codes.TransportEquipmentCategoryCode;
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
import org.xml.sax.SAXException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentifiersRequestServiceTest extends BaseServiceTest {
    private static final String DATA_UUID = "12345678-ab12-4ab6-8999-123456789abc";
    private static final String PLATFORM_ID = "ttf";

    @Mock
    private IdentifiersService identifiersService;
    @Mock
    private IdentifiersRequestRepository identifiersRequestRepository;
    @Mock
    private IdentifiersControlUpdateDelegateService identifiersControlUpdateDelegateService;
    private IdentifiersRequestService identifiersRequestService;
    @Captor
    private ArgumentCaptor<IdentifiersRequestDto> requestDtoArgumentCaptor;
    @Captor
    private ArgumentCaptor<IdentifiersRequestEntity> requestEntityArgumentCaptor;
    private ConsignmentDto consignmentDto;
    private final IdentifiersRequestEntity identifiersRequestEntity = new IdentifiersRequestEntity();
    private final IdentifiersRequestEntity secondIdentifiersRequestEntity = new IdentifiersRequestEntity();
    private final IdentifiersRequestDto identifiersRequestDto = new IdentifiersRequestDto();

    @Mock
    private final ValidationService validationService = new ValidationService();


    @Override
    @BeforeEach
    public void before() {
        super.before();
        super.setDtoRequestCommonAttributes(identifiersRequestDto);
        super.setEntityRequestCommonAttributes(identifiersRequestEntity);
        super.setEntityRequestCommonAttributes(secondIdentifiersRequestEntity);
        controlEntity.setRequests(List.of(identifiersRequestEntity, secondIdentifiersRequestEntity));

        consignmentDto = ConsignmentDto.builder()
                .datasetId(DATA_UUID)
                .platformId(PLATFORM_ID)
                .usedTransportEquipments(List.of(UsedTransportEquipmentDto.builder()
                        .equipmentId("abc123")
                        .registrationCountry("FR")
                        .categoryCode(TransportEquipmentCategoryCode.BPQ.value())
                        .build(), UsedTransportEquipmentDto.builder()
                        .equipmentId("abc124").registrationCountry("BE").build()))
                .build();

        identifiersRequestService = new IdentifiersRequestService(identifiersRequestRepository, mapperUtils, rabbitSenderService, controlService, gateProperties,
                identifiersService, requestUpdaterService, serializeUtils, logManager, identifiersControlUpdateDelegateService, validationService);
    }

    @Test
    void shouldCreateAndSendRequest() {
        //Arrange
        when(identifiersRequestRepository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        //Act
        identifiersRequestService.createAndSendRequest(controlDto, "https://efti.platform.borduria.eu");

        //Assert
        verify(mapperUtils, times(1)).requestDtoToRequestEntity(requestDtoArgumentCaptor.capture(), eq(IdentifiersRequestEntity.class));
        assertEquals("https://efti.platform.borduria.eu", requestDtoArgumentCaptor.getValue().getGateIdDest());
    }

    @Test
    void shouldCreateRequest() {
        //Arrange
        when(identifiersRequestRepository.save(any())).thenReturn(identifiersRequestEntity);

        //Act
        identifiersRequestService.createRequest(controlDto, SUCCESS, Collections.singletonList(consignmentDto));

        //Assert
        verify(mapperUtils).requestDtoToRequestEntity(requestDtoArgumentCaptor.capture(), eq(IdentifiersRequestEntity.class));
        assertEquals(identifiersResult.getDatasetId(), requestDtoArgumentCaptor.getValue().getIdentifiersResults().getConsignments().get(0).getDatasetId());
        assertEquals(identifiersResult.getPlatformId(), requestDtoArgumentCaptor.getValue().getIdentifiersResults().getConsignments().get(0).getPlatformId());
        assertEquals(identifiersResult.getUsedTransportEquipments().size(), requestDtoArgumentCaptor.getValue().getIdentifiersResults().getConsignments().get(0).getUsedTransportEquipments().size());
        assertEquals(SUCCESS, requestDtoArgumentCaptor.getValue().getStatus());
    }

    @Test
    void trySendDomibusSuccessTest() throws SendRequestException, JsonProcessingException {
        identifiersRequestService.sendRequest(requestDto);
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
        when(controlService.createControlFrom(any(), any())).thenReturn(controlDto);
        when(controlService.updateControl(any())).thenReturn(controlDto);
        when(identifiersRequestRepository.save(any())).thenReturn(identifiersRequestEntity);
        //Act
        identifiersRequestService.manageQueryReceived(notificationDto);

        //assert
        verify(controlService).createControlFrom(any(), any());
        verify(identifiersRequestRepository, times(2)).save(any());
        verify(identifiersService).search(any());
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
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH);
        identifiersRequestEntity.setStatus(IN_PROGRESS);
        controlEntity.setRequests(List.of(identifiersRequestEntity));

        when(controlService.findByRequestId(any())).thenReturn(Optional.of(controlEntity));
        when(identifiersRequestRepository.findByControlRequestIdAndGateIdDest(any(), any())).thenReturn(identifiersRequestEntity);

        //Act
        identifiersRequestService.manageResponseReceived(notificationDto);

        //assert
        verify(identifiersControlUpdateDelegateService).updateExistingControl(any(), anyString());
        verify(identifiersControlUpdateDelegateService).setControlNextStatus("67fe38bd-6bf7-4b06-b20e-206264bd639c");
    }

    @Test
    void allRequestsContainsDataTest_whenFalse() {
        assertFalse(identifiersRequestService.allRequestsContainsData(List.of(identifiersRequestEntity)));
    }

    @Test
    void allRequestsContainsDataTest_whenTrue() {
        //Arrange
        identifiersRequestEntity.setIdentifiersResults(IdentifiersResults.builder().consignments(List.of(consignmentDto)).build());
        //Act and Assert
        assertTrue(identifiersRequestService.allRequestsContainsData(List.of(identifiersRequestEntity)));
    }

    @Test
    void shouldUpdateControlAndRequestStatus_whenResponseSentSuccessfullyForExternalRequest() {
        identifiersRequestEntity.setEdeliveryMessageId(MESSAGE_ID);
        when(identifiersRequestRepository.findByControlRequestTypeAndStatusAndEdeliveryMessageId(any(), any(), any())).thenReturn(identifiersRequestEntity);

        identifiersRequestService.manageSendSuccess(MESSAGE_ID);

        verify(identifiersRequestRepository).save(requestEntityArgumentCaptor.capture());
        assertEquals(COMPLETE, requestEntityArgumentCaptor.getValue().getControl().getStatus());
        assertEquals(SUCCESS, requestEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void shouldNotUpdateControlAndRequestStatus_AndLogMessage_whenResponseSentSuccessfully() {
        identifiersRequestEntity.setEdeliveryMessageId(MESSAGE_ID);
        identifiersRequestService.manageSendSuccess(MESSAGE_ID);

        identifiersRequestService.manageSendSuccess(MESSAGE_ID);
        verify(identifiersRequestRepository, never()).save(any());
    }

    @Test
    void shouldUpdateRequestStatus_whenRequestIsExternal() {
        identifiersRequestDto.getControl().setRequestType(RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH);
        when(mapperUtils.requestToRequestDto(identifiersRequestEntity, IdentifiersRequestDto.class)).thenReturn(identifiersRequestDto);
        when(mapperUtils.requestDtoToRequestEntity(identifiersRequestDto, IdentifiersRequestEntity.class)).thenReturn(identifiersRequestEntity);
        when(identifiersRequestRepository.save(any())).thenReturn(identifiersRequestEntity);

        identifiersRequestService.updateRequestStatus(identifiersRequestDto, MESSAGE_ID);

        verify(mapperUtils, times(1)).requestDtoToRequestEntity(requestDtoArgumentCaptor.capture(), eq(IdentifiersRequestEntity.class));
        assertEquals(RESPONSE_IN_PROGRESS, identifiersRequestDto.getStatus());
    }

    @Test
    void shouldUpdateRequestStatus_whenRequestIsNotExternal() {
        identifiersRequestDto.getControl().setRequestType(RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH);
        when(mapperUtils.requestToRequestDto(identifiersRequestEntity, IdentifiersRequestDto.class)).thenReturn(identifiersRequestDto);
        when(mapperUtils.requestDtoToRequestEntity(identifiersRequestDto, IdentifiersRequestEntity.class)).thenReturn(identifiersRequestEntity);
        when(identifiersRequestRepository.save(any())).thenReturn(identifiersRequestEntity);

        identifiersRequestService.updateRequestStatus(identifiersRequestDto, MESSAGE_ID);

        verify(mapperUtils, times(1)).requestDtoToRequestEntity(requestDtoArgumentCaptor.capture(), eq(IdentifiersRequestEntity.class));
        assertEquals(IN_PROGRESS, identifiersRequestDto.getStatus());
    }

    @Test
    void shouldBuildRequestBody_whenRemoteGateSentResponse() {
        controlDto.setRequestType(RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH);
        controlDto.setIdentifiersResults(identifiersResultsDto.getConsignments());
        final RabbitRequestDto rabbitRequestDto = new RabbitRequestDto();
        rabbitRequestDto.setControl(controlDto);
        rabbitRequestDto.setIdentifiersResults(IdentifiersResults.builder().consignments(List.of(consignmentDto)).build());
        final String expectedRequestBody = testFile("/xml/FTI021.xml");

        final String requestBody = identifiersRequestService.buildRequestBody(rabbitRequestDto);

        assertThat(expectedRequestBody, CompareMatcher.isIdenticalTo(requestBody).ignoreWhitespace());
    }

    @Test
    void shouldBuildRequestBody_whenLocalGateSendsRequest() {
        controlDto.setRequestType(RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH);
        controlDto.setIdentifiersResults(identifiersResultsDto.getConsignments());
        controlDto.setTransportIdentifiers(searchParameter);
        final RabbitRequestDto rabbitRequestDto = new RabbitRequestDto();
        rabbitRequestDto.setControl(controlDto);
        final String expectedRequestBody = testFile("/xml/FTI013.xml");

        final String requestBody = identifiersRequestService.buildRequestBody(rabbitRequestDto);

        assertThat(expectedRequestBody, CompareMatcher.isIdenticalTo(requestBody).ignoreWhitespace());
    }

    @Test
    void shouldFindRequestByMessageId_whenRequestExists() {
        when(identifiersRequestRepository.findByEdeliveryMessageId(anyString())).thenReturn(identifiersRequestEntity);
        final IdentifiersRequestEntity requestByMessageId = identifiersRequestService.findRequestByMessageIdOrThrow(MESSAGE_ID);
        assertNotNull(requestByMessageId);
    }

    @Test
    void shouldThrowException_whenFindRequestByMessageId_andRequestDoesNotExists() {
        final Exception exception = assertThrows(RequestNotFoundException.class, () -> {
            identifiersRequestService.findRequestByMessageIdOrThrow(MESSAGE_ID);
        });
        assertEquals("couldn't find Consignment request for messageId: messageId", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForRequestTypeEnumSupport")
    void supports_ShouldReturnTrueForIdentifiers(final RequestTypeEnum requestTypeEnum, final boolean expectedResult) {
        assertEquals(expectedResult, identifiersRequestService.supports(requestTypeEnum));
    }

    private static Stream<Arguments> getArgumentsForRequestTypeEnumSupport() {
        return Stream.of(
                Arguments.of(RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH, true),
                Arguments.of(RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH, true),
                Arguments.of(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH, false),
                Arguments.of(RequestTypeEnum.EXTERNAL_UIL_SEARCH, false),
                Arguments.of(RequestTypeEnum.EXTERNAL_NOTE_SEND, false),
                Arguments.of(RequestTypeEnum.LOCAL_IDENTIFIERS_SEARCH, true),
                Arguments.of(RequestTypeEnum.LOCAL_UIL_SEARCH, false)
        );
    }

    @Test
    void shouldGetRequestForControlId() {
        identifiersRequestService.findAllForControlId(1);
        verify(identifiersRequestRepository).findByControlId(1);
    }

    @Test
    void shouldCreateOrUpdateIdentifiers() {
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .messageId(MESSAGE_ID)
                        .body(testFile("/xml/SaveIdentifierRequest.xml"))
                        .build())
                .build();
        identifiersRequestService.createOrUpdate(notificationDto);

        verify(identifiersService).createOrUpdate(any(SaveIdentifiersRequestWrapper.class));
    }

    @Test
    void shouldNotCreateOrUpdateIdentifiersIfInvalid() throws IOException, SAXException {
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .messageId(MESSAGE_ID)
                        .body(testFile("/xml/SaveIdentifierRequest.xml"))
                        .build())
                .build();
        doThrow(new SAXException("Error occurred")).when(validationService).validateXml(anyString());
        identifiersRequestService.createOrUpdate(notificationDto);

        verify(identifiersService, never()).createOrUpdate(any(SaveIdentifiersRequestWrapper.class));
    }


}

