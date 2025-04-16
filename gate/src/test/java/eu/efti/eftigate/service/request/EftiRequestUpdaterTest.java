package eu.efti.eftigate.service.request;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.NotificationType;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.UilRequestEntity;
import eu.efti.eftigate.repository.RequestRepository;
import eu.efti.eftigate.service.BaseServiceTest;
import eu.efti.eftilogger.service.ReportingRequestLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EftiRequestUpdaterTest extends BaseServiceTest {

    @Mock
    private RequestRepository<UilRequestEntity> requestRepository;
    @Mock
    private RequestServiceFactory requestServiceFactory;
    @Mock
    private UilRequestService uilRequestService;
    @Mock
    private ReportingRequestLogService reportingRequestLogService;
    @InjectMocks
    private EftiRequestUpdater eftiRequestUpdater;

    private UilRequestEntity requestEntity;

    @Override
    @BeforeEach
    public void before() {
        super.before();
        requestEntity = new UilRequestEntity();
        requestEntity.setId(1L);
        requestEntity.setEdeliveryMessageId("messageId");
        requestEntity.setRequestType("UIL");
        final ControlEntity controlEntity = new ControlEntity();
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH);
        requestEntity.setControl(controlEntity);
        controlEntity.setRequests(List.of(requestEntity));
    }

    @Test
    void shouldUpdateResponseSendFailure() {
        final String messageId = "messageId";
        requestEntity.setEdeliveryMessageId(messageId);
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.SEND_FAILURE)
                .messageId(messageId)
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .build())
                .build();
        when(requestRepository.findByEdeliveryMessageId(any())).thenReturn(requestEntity);

        eftiRequestUpdater.manageSendFailure(notificationDto, "test");

        verify(controlService).save(any(ControlDto.class));
    }

    @Test
    void shouldManageResponseSendSuccess() {
        final String messageId = "messageId";
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.SEND_SUCCESS)
                .messageId(messageId)
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .build())
                .build();
        when(requestRepository.findByEdeliveryMessageId(any())).thenReturn(requestEntity);
        when(requestServiceFactory.getRequestServiceByRequestType(any(String.class))).thenReturn(uilRequestService);

        eftiRequestUpdater.manageSendSuccess(notificationDto, "test");

        verify(uilRequestService).manageSendSuccess(messageId);
    }

}
