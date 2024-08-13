package com.ingroupe.efti.eftigate.service.request;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.ingroupe.common.test.log.MemoryAppender;
import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.enums.RequestTypeEnum;
import com.ingroupe.efti.commons.enums.StatusEnum;
import com.ingroupe.efti.eftigate.entity.ControlEntity;
import com.ingroupe.efti.eftigate.entity.IdentifiersRequestEntity;
import com.ingroupe.efti.eftigate.repository.IdentifiersRequestRepository;
import com.ingroupe.efti.eftigate.service.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static com.ingroupe.efti.eftigate.EftiTestUtils.testFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentifiersControlUpdateDelegateServiceTest extends BaseServiceTest {
    protected final ControlEntity controlEntity = new ControlEntity();
    private final IdentifiersRequestEntity identifiersRequestEntity = new IdentifiersRequestEntity();
    private final IdentifiersRequestEntity secondIdentifiersRequestEntity = new IdentifiersRequestEntity();

    private IdentifiersControlUpdateDelegateService identifiersControlUpdateDelegateService;
    @Mock
    private IdentifiersRequestRepository identifiersRequestRepository;
    @Captor
    private ArgumentCaptor<IdentifiersRequestEntity> requestEntityArgumentCaptor;
    @Captor
    private ArgumentCaptor<ControlEntity> controlEntityArgumentCaptor;

    @Override
    @BeforeEach
    public void before() {
        super.before();
        super.setEntityRequestCommonAttributes(identifiersRequestEntity);
        super.setEntityRequestCommonAttributes(secondIdentifiersRequestEntity);
        controlEntity.setRequests(List.of(identifiersRequestEntity));
        identifiersControlUpdateDelegateService = new IdentifiersControlUpdateDelegateService(identifiersRequestRepository, serializeUtils, controlService, mapperUtils);


        final Logger memoryAppenderTestLogger = (Logger) LoggerFactory.getLogger(MetadataRequestService.class);
        memoryAppender = MemoryAppender.createInitializedMemoryAppender(Level.INFO, memoryAppenderTestLogger);
    }

    @Test
    void shouldUpdateExistingControlRequest() {
        //Arrange
        identifiersRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        when(identifiersRequestRepository.findByControlRequestUuidAndStatusAndGateUrlDest("67fe38bd-6bf7-4b06-b20e-206264bd639c", RequestStatusEnum.IN_PROGRESS, "https://efti.platform.borduria.eu")).thenReturn(identifiersRequestEntity);
        //Act
        identifiersControlUpdateDelegateService.updateExistingControl(testFile("/xml/FTI021-full.xml"), "67fe38bd-6bf7-4b06-b20e-206264bd639c", "https://efti.platform.borduria.eu");

        //Assert
        verify(identifiersRequestRepository).save(requestEntityArgumentCaptor.capture());
        assertEquals(RequestStatusEnum.SUCCESS, requestEntityArgumentCaptor.getValue().getStatus());
        assertNotNull(requestEntityArgumentCaptor.getValue().getMetadataResults());
        assertFalse(requestEntityArgumentCaptor.getValue().getMetadataResults().getMetadataResult().isEmpty());
    }

    @Test
    void shouldGetCompleteAsControlNextStatus_whenAllRequestsAreInSuccessStatus() {
        identifiersRequestEntity.setStatus(RequestStatusEnum.SUCCESS);
        controlEntity.setStatus(StatusEnum.PENDING);
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_METADATA_SEARCH);
        when(identifiersRequestRepository.findByControlRequestUuid("67fe38bd-6bf7-4b06-b20e-206264bd639c")).thenReturn(List.of(identifiersRequestEntity));
        when(controlService.findByRequestUuid("67fe38bd-6bf7-4b06-b20e-206264bd639c")).thenReturn(Optional.of(controlEntity));
        //Act
        identifiersControlUpdateDelegateService.setControlNextStatus("67fe38bd-6bf7-4b06-b20e-206264bd639c");

        //assert
        verify(controlService).save(controlEntityArgumentCaptor.capture());
        assertEquals(StatusEnum.COMPLETE, controlEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void shouldGetErrorAsControlNextStatus_whenSomeRequestsAreInErrorStatus() {
        identifiersRequestEntity.setStatus(RequestStatusEnum.ERROR);
        controlEntity.setStatus(StatusEnum.PENDING);
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_METADATA_SEARCH);
        when(identifiersRequestRepository.findByControlRequestUuid("67fe38bd-6bf7-4b06-b20e-206264bd639c")).thenReturn(List.of(identifiersRequestEntity, secondIdentifiersRequestEntity));
        when(controlService.findByRequestUuid("67fe38bd-6bf7-4b06-b20e-206264bd639c")).thenReturn(Optional.of(controlEntity));
        //Act
        identifiersControlUpdateDelegateService.setControlNextStatus("67fe38bd-6bf7-4b06-b20e-206264bd639c");

        //assert
        verify(controlService).save(controlEntityArgumentCaptor.capture());
        assertEquals(StatusEnum.ERROR, controlEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void shouldGetTimeoutAsControlNextStatus_whenSomeRequestsAreInTimeoutStatus() {
        secondIdentifiersRequestEntity.setStatus(RequestStatusEnum.TIMEOUT);
        identifiersRequestEntity.setStatus(RequestStatusEnum.IN_PROGRESS);
        controlEntity.setStatus(StatusEnum.PENDING);
        controlEntity.setRequests(List.of(identifiersRequestEntity, secondIdentifiersRequestEntity));
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_METADATA_SEARCH);
        when(identifiersRequestRepository.findByControlRequestUuid("67fe38bd-6bf7-4b06-b20e-206264bd639c")).thenReturn(List.of(identifiersRequestEntity, secondIdentifiersRequestEntity));
        when(controlService.findByRequestUuid("67fe38bd-6bf7-4b06-b20e-206264bd639c")).thenReturn(Optional.of(controlEntity));
        //Act
        identifiersControlUpdateDelegateService.setControlNextStatus("67fe38bd-6bf7-4b06-b20e-206264bd639c");

        //assert
        verify(controlService).save(controlEntityArgumentCaptor.capture());
        assertEquals(StatusEnum.TIMEOUT, controlEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void  shouldGetErrorAsControlNextStatusOverTimeout_whenAtLeastOneRequestIsInErrorStatus() {
        secondIdentifiersRequestEntity.setStatus(RequestStatusEnum.TIMEOUT);
        identifiersRequestEntity.setStatus(RequestStatusEnum.ERROR);
        controlEntity.setStatus(StatusEnum.PENDING);
        controlEntity.setRequests(List.of(identifiersRequestEntity, secondIdentifiersRequestEntity));
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_METADATA_SEARCH);
        when(identifiersRequestRepository.findByControlRequestUuid("67fe38bd-6bf7-4b06-b20e-206264bd639c")).thenReturn(List.of(identifiersRequestEntity, secondIdentifiersRequestEntity));
        when(controlService.findByRequestUuid("67fe38bd-6bf7-4b06-b20e-206264bd639c")).thenReturn(Optional.of(controlEntity));
        //Act
        identifiersControlUpdateDelegateService.setControlNextStatus("67fe38bd-6bf7-4b06-b20e-206264bd639c");

        //assert
        verify(controlService).save(controlEntityArgumentCaptor.capture());
        assertEquals(StatusEnum.ERROR, controlEntityArgumentCaptor.getValue().getStatus());
    }

}
