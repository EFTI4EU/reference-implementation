package eu.efti.eftigate.service.request;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.utils.MemoryAppender;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.service.RabbitSenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static eu.efti.commons.enums.RequestStatusEnum.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    RabbitSenderService rabbitSenderService;

    private MemoryAppender memoryAppender;

    private Logger memoryAppenderTestLogger;

    private static final String LOGGER_NAME = RequestService.class.getName();

    @Captor
    private ArgumentCaptor<RequestDto> requestDtoArgumentCaptor;


    @BeforeEach
    void before() {
        memoryAppenderTestLogger = (Logger) LoggerFactory.getLogger(LOGGER_NAME);
        memoryAppender = MemoryAppender.createInitializedMemoryAppender(
                Level.TRACE, memoryAppenderTestLogger);
    }

    @Test
    void buildErrorRequestDtoTest() {
        GateProperties gateProperties = GateProperties.builder().owner("owner").build();
        RequestService requestService = Mockito.mock(RequestService.class, Mockito.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(requestService, "gateProperties", gateProperties);

        NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder().conversationId("je suis une conversation").fromPartyId("fromPartyId").build()).build();

        RequestDto requestDto = requestService.buildErrorRequestDto(notificationDto, RequestTypeEnum.EXTERNAL_UIL_SEARCH);

        assertEquals("je suis une conversation", requestDto.getControl().getRequestId());
        assertNotNull(requestDto.getError());
    }

    @Test
    void createRequestTest() {
        RequestService requestService = Mockito.mock(RequestService.class, Mockito.CALLS_REAL_METHODS);
        ControlDto controlDto = ControlDto.builder().requestId("oki").build();
        final String message = "Request has been registered with controlId : 0";

        when(requestService.save(any())).thenReturn(RequestDto.builder().control(controlDto).build());

        requestService.createRequest(new ControlDto(), SUCCESS);

        assertTrue(memoryAppender.containsFormattedLogMessage(message));
        assertEquals(1, memoryAppender.countEventsForLogger(LOGGER_NAME, Level.INFO));
    }

    @Test
    void notifyTimeoutTest() {
        RequestService requestService = Mockito.mock(RequestService.class, Mockito.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(requestService, "rabbitSenderService", rabbitSenderService);

        requestService.notifyTimeout(RequestDto.builder().control(ControlDto.builder().requestId("requestId").build()).build());

        verify(requestService).sendRequest(requestDtoArgumentCaptor.capture());
        assertEquals("requestId", requestDtoArgumentCaptor.getValue().getControl().getRequestId());
    }
}
