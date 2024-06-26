package eu.efti.eftigate.service.request;

import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.NotificationType;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.exception.RequestNotFoundException;
import eu.efti.eftigate.service.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EftiRequestUpdaterTest extends BaseServiceTest {

    @InjectMocks
    private EftiRequestUpdater eftiRequestUpdater;

    @Override
    @BeforeEach
    public void before() {
        super.before();
    }

    @Test
    void shouldThrowIfMessageNotFound() {
        final String messageId = "messageId";
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.SEND_FAILURE)
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .build())
                .build();
        assertThrows(RequestNotFoundException.class, () -> eftiRequestUpdater.manageSendFailure(notificationDto));
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
        final ArgumentCaptor<ControlEntity> controlEntityArgumentCaptor = ArgumentCaptor.forClass(ControlEntity.class);
        when(requestRepository.findByEdeliveryMessageId(any())).thenReturn(requestEntity);

        eftiRequestUpdater.manageSendFailure(notificationDto);

        verify(controlService).save(controlEntityArgumentCaptor.capture());
        assertEquals(RequestStatusEnum.SEND_ERROR, controlEntityArgumentCaptor.getValue().getRequests().iterator().next().getStatus());
    }

    @Test
    void shouldManageResponseSendSuccess() {
        final String messageId = "messageId";
        requestEntity.setEdeliveryMessageId(messageId);
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.SEND_SUCCESS)
                .messageId(messageId)
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .build())
                .build();
        final ArgumentCaptor<ControlEntity> controlEntityArgumentCaptor = ArgumentCaptor.forClass(ControlEntity.class);
        when(requestRepository.findByControlRequestTypeInAndStatusAndEdeliveryMessageId(anyList(), any(), any())).thenReturn(requestEntity);

        eftiRequestUpdater.manageSendSuccess(notificationDto);

        verify(controlService).save(controlEntityArgumentCaptor.capture());
        assertEquals(RequestStatusEnum.SUCCESS, controlEntityArgumentCaptor.getValue().getRequests().iterator().next().getStatus());
    }

}
