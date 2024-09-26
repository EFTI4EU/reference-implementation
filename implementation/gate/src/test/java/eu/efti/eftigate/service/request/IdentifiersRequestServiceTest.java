package eu.efti.eftigate.service.request;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import eu.efti.commons.dto.IdentifiersDto;
import eu.efti.commons.dto.IdentifiersRequestDto;
import eu.efti.commons.dto.IdentifiersResponseDto;
import eu.efti.commons.dto.TransportVehicleDto;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.commons.enums.EDeliveryAction;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.NotificationType;
import eu.efti.edeliveryapconnector.exception.SendRequestException;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.IdentifiersRequestEntity;
import eu.efti.eftigate.entity.IdentifiersResult;
import eu.efti.eftigate.entity.IdentifiersResults;
import eu.efti.eftigate.entity.TransportVehicleEntity;
import eu.efti.eftigate.exception.RequestNotFoundException;
import eu.efti.eftigate.repository.IdentifiersRequestRepository;
import eu.efti.eftigate.service.BaseServiceTest;
import eu.efti.identifiersregistry.service.IdentifiersService;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentifiersRequestServiceTest extends BaseServiceTest {
    private static final String DATA_UUID = "12345678-ab12-4ab6-8999-123456789abc";
    private static final String PLATFORM_URL = "http://efti.platform.truc.eu";

    @Mock
    private IdentifiersService identifiersService;
    @Mock
    private IdentifiersRequestRepository identifiersRequestRepository;
    @Mock
    private IdentifiersControlUpdateDelegateService identifiersControlUpdateDelegateService;
    private IdentifiersRequestService IdentifiersRequestService;
    @Captor
    private ArgumentCaptor<IdentifiersRequestDto> requestDtoArgumentCaptor;
    @Captor
    private ArgumentCaptor<IdentifiersRequestEntity> requestEntityArgumentCaptor;
    @Captor
    private ArgumentCaptor<ControlEntity> controlEntityArgumentCaptor;
    private IdentifiersDto identifiersDto;
    private IdentifiersResult identifiersResult;
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

        identifiersDto = IdentifiersDto.builder()
                .eFTIDataUuid(DATA_UUID)
                .eFTIPlatformUrl(PLATFORM_URL)
                .transportVehicles(List.of(TransportVehicleDto.builder()
                        .vehicleID("abc123").countryStart("FR").vehicleCountry("FR").countryEnd("toto").build(), TransportVehicleDto.builder()
                        .vehicleID("abc124").countryStart("BE").vehicleCountry("BE").countryEnd("IT").build())).build();

        identifiersResult = IdentifiersResult.builder()
                .eFTIDataUuid(DATA_UUID)
                .eFTIPlatformUrl(PLATFORM_URL)
                .transportVehicles(List.of(TransportVehicleEntity.builder()
                        .vehicleID("abc123").vehicleCountry(CountryIndicator.FR).build(), TransportVehicleEntity.builder()
                        .vehicleID("abc124").vehicleCountry(CountryIndicator.BE).build())).build();
        IdentifiersRequestService = new IdentifiersRequestService(identifiersRequestRepository, mapperUtils, rabbitSenderService, controlService, gateProperties,
                identifiersService, requestUpdaterService, serializeUtils, logManager, identifiersControlUpdateDelegateService);
    }

    @Test
    void shouldCreateAndSendRequest() {
        //Arrange
        when(identifiersRequestRepository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        //Act
        IdentifiersRequestService.createAndSendRequest(controlDto, "https://efti.platform.borduria.eu");

        //Assert
        verify(mapperUtils, times(1)).requestDtoToRequestEntity(requestDtoArgumentCaptor.capture(), eq(IdentifiersRequestEntity.class));
        assertEquals("https://efti.platform.borduria.eu", requestDtoArgumentCaptor.getValue().getGateUrlDest());
    }

    @Test
    void shouldCreateRequest() {
        //Arrange
        when(identifiersRequestRepository.save(any())).thenReturn(identifiersRequestEntity);

        //Act
        IdentifiersRequestService.createRequest(controlDto, SUCCESS, Collections.singletonList(identifiersDto));

        //Assert
        verify(mapperUtils).requestDtoToRequestEntity(requestDtoArgumentCaptor.capture(), eq(IdentifiersRequestEntity.class));
        assertEquals(identifiersResult.getIdentifiersUUID(), requestDtoArgumentCaptor.getValue().getIdentifiersResultsDto().getIdentifiersResult().get(0).getIdentifiersUUID());
        assertEquals(identifiersResult.getEFTIDataUuid(), requestDtoArgumentCaptor.getValue().getIdentifiersResultsDto().getIdentifiersResult().get(0).getEFTIDataUuid());
        assertEquals(identifiersResult.getEFTIPlatformUrl(), requestDtoArgumentCaptor.getValue().getIdentifiersResultsDto().getIdentifiersResult().get(0).getEFTIPlatformUrl());
        assertEquals(identifiersResult.getTransportVehicles().size(), requestDtoArgumentCaptor.getValue().getIdentifiersResultsDto().getIdentifiersResult().get(0).getTransportVehicles().size());
        assertEquals(SUCCESS, requestDtoArgumentCaptor.getValue().getStatus());
    }

    @Test
    void trySendDomibusSuccessTest() throws SendRequestException, JsonProcessingException {
        IdentifiersRequestService.sendRequest(requestDto);
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
        IdentifiersRequestService.manageMessageReceive(notificationDto);

        //assert
        verify(controlService).createControlFrom(any(), any(), any());
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
        when(controlService.existsByCriteria("67fe38bd-6bf7-4b06-b20e-206264bd639c")).thenReturn(true);

        //Act
        IdentifiersRequestService.manageMessageReceive(notificationDto);

        //assert
        verify(identifiersControlUpdateDelegateService).updateExistingControl(anyString(), anyString(), anyString());
        verify(identifiersControlUpdateDelegateService).setControlNextStatus("67fe38bd-6bf7-4b06-b20e-206264bd639c");
    }

    @Test
    void allRequestsContainsDataTest_whenFalse() {
        assertFalse(IdentifiersRequestService.allRequestsContainsData(List.of(identifiersRequestEntity)));
    }

    @Test
    void allRequestsContainsDataTest_whenTrue() {
        //Arrange
        identifiersRequestEntity.setIdentifiersResults(new IdentifiersResults(List.of(identifiersResult)));
        //Act and Assert
        assertTrue(IdentifiersRequestService.allRequestsContainsData(List.of(identifiersRequestEntity)));
    }

    @Test
    void shouldUpdateControlAndRequestStatus_whenResponseSentSuccessfullyForExternalRequest() {
        identifiersRequestEntity.setEdeliveryMessageId(MESSAGE_ID);
        when(identifiersRequestRepository.findByControlRequestTypeAndStatusAndEdeliveryMessageId(any(), any(), any())).thenReturn(identifiersRequestEntity);

        IdentifiersRequestService.manageSendSuccess(MESSAGE_ID);

        verify(identifiersRequestRepository).save(requestEntityArgumentCaptor.capture());
        assertEquals(COMPLETE, requestEntityArgumentCaptor.getValue().getControl().getStatus());
        assertEquals(SUCCESS, requestEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void shouldNotUpdateControlAndRequestStatus_AndLogMessage_whenResponseSentSuccessfully() {
        identifiersRequestEntity.setEdeliveryMessageId(MESSAGE_ID);
        IdentifiersRequestService.manageSendSuccess(MESSAGE_ID);
    }

    @Test
    void shouldUpdateSentRequestStatus_whenRequestIsExternal(){
        identifiersRequestDto.getControl().setRequestType(RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH);
        when(mapperUtils.requestToRequestDto(identifiersRequestEntity, IdentifiersRequestDto.class)).thenReturn(identifiersRequestDto);
        when(mapperUtils.requestDtoToRequestEntity(identifiersRequestDto, IdentifiersRequestEntity.class)).thenReturn(identifiersRequestEntity);
        when(identifiersRequestRepository.save(any())).thenReturn(identifiersRequestEntity);

        IdentifiersRequestService.updateSentRequestStatus(identifiersRequestDto, MESSAGE_ID);

        verify(mapperUtils, times(1)).requestDtoToRequestEntity(requestDtoArgumentCaptor.capture(), eq(IdentifiersRequestEntity.class));
        assertEquals(RESPONSE_IN_PROGRESS, identifiersRequestDto.getStatus());
    }

    @Test
    void shouldUpdateSentRequestStatus_whenRequestIsNotExternal(){
        identifiersRequestDto.getControl().setRequestType(RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH);
        when(mapperUtils.requestToRequestDto(identifiersRequestEntity, IdentifiersRequestDto.class)).thenReturn(identifiersRequestDto);
        when(mapperUtils.requestDtoToRequestEntity(identifiersRequestDto, IdentifiersRequestEntity.class)).thenReturn(identifiersRequestEntity);
        when(identifiersRequestRepository.save(any())).thenReturn(identifiersRequestEntity);

        IdentifiersRequestService.updateSentRequestStatus(identifiersRequestDto, MESSAGE_ID);

        verify(mapperUtils, times(1)).requestDtoToRequestEntity(requestDtoArgumentCaptor.capture(), eq(IdentifiersRequestEntity.class));
        assertEquals(IN_PROGRESS, identifiersRequestDto.getStatus());
    }

    @Test
    void shouldBuildRequestBody_whenRemoteGateSentResponse(){
        controlDto.setRequestType(RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH);
        controlDto.setIdentifiersResults(identifiersResultsDto);
        final RabbitRequestDto rabbitRequestDto = new RabbitRequestDto();
        rabbitRequestDto.setControl(controlDto);
        final IdentifiersResponseDto identifiersResponseDto = IdentifiersResponseDto.builder()
                .requestUuid(controlDto.getRequestUuid())
                .status(controlDto.getStatus())
                .identifiers(List.of(identifiersResultDto)).build();

        final String expectedRequestBody = testFile("/xml/FTI021.xml");

        when(controlService.buildIdentifiersResponse(any(), anyList())).thenReturn(identifiersResponseDto);

        final String requestBody = IdentifiersRequestService.buildRequestBody(rabbitRequestDto);

        assertThat(StringUtils.deleteWhitespace(expectedRequestBody), CompareMatcher.isIdenticalTo(requestBody));
    }

    @Test
    void shouldBuildRequestBody_whenLocalGateSendsRequest(){
        controlDto.setRequestType(RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH);
        controlDto.setIdentifiersResults(identifiersResultsDto);
        controlDto.setTransportIdentifiers(searchParameter);
        final RabbitRequestDto rabbitRequestDto = new RabbitRequestDto();
        rabbitRequestDto.setControl(controlDto);
        final String expectedRequestBody = testFile("/xml/FTI013.xml");

        final String requestBody = IdentifiersRequestService.buildRequestBody(rabbitRequestDto);

        assertThat(StringUtils.deleteWhitespace(expectedRequestBody), CompareMatcher.isIdenticalTo(requestBody));
    }

    @Test
    void shouldFindRequestByMessageId_whenRequestExists(){
        when(identifiersRequestRepository.findByEdeliveryMessageId(anyString())).thenReturn(identifiersRequestEntity);
        final IdentifiersRequestEntity requestByMessageId = IdentifiersRequestService.findRequestByMessageIdOrThrow(MESSAGE_ID);
        assertNotNull(requestByMessageId);
    }

    @Test
    void shouldThrowException_whenFindRequestByMessageId_andRequestDoesNotExists() {
        final Exception exception = assertThrows(RequestNotFoundException.class, () -> {
            IdentifiersRequestService.findRequestByMessageIdOrThrow(MESSAGE_ID);
        });
        assertEquals("couldn't find Consignment request for messageId: messageId", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForEdeliveryActionSupport")
    void supports_ShouldReturnTrueForIdentifiers(final EDeliveryAction eDeliveryAction, final boolean expectedResult) {
        assertEquals(expectedResult, IdentifiersRequestService.supports(eDeliveryAction));
    }

    private static Stream<Arguments> getArgumentsForEdeliveryActionSupport() {
        return Stream.of(
                Arguments.of(EDeliveryAction.GET_IDENTIFIERS, true),
                Arguments.of(EDeliveryAction.SEND_NOTES, false),
                Arguments.of(EDeliveryAction.GET_UIL, false),
                Arguments.of(EDeliveryAction.UPLOAD_IDENTIFIERS, false),
                Arguments.of(EDeliveryAction.FORWARD_UIL, false)
        );
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForRequestTypeEnumSupport")
    void supports_ShouldReturnTrueForIdentifiers(final RequestTypeEnum requestTypeEnum, final boolean expectedResult) {
        assertEquals(expectedResult, IdentifiersRequestService.supports(requestTypeEnum));
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

}

