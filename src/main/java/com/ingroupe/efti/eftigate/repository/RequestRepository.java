package com.ingroupe.efti.eftigate.repository;

import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.enums.RequestTypeEnum;
import com.ingroupe.efti.eftigate.entity.RequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RequestRepository<T extends RequestEntity> extends JpaRepository<T, Long> {

    T findByEdeliveryMessageId(final String messageId);

    T findByControlRequestTypeAndStatusAndEdeliveryMessageId(final RequestTypeEnum controlRequestType, final RequestStatusEnum requestStatusEnum, final String messageId);

    @Transactional("controlTransactionManager")
    List<T> findByControlRequestUuid(final String controlRequestUuid);

}
