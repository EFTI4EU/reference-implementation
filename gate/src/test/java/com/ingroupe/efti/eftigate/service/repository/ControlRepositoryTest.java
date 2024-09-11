package com.ingroupe.efti.eftigate.service.repository;

import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.enums.StatusEnum;
import com.ingroupe.efti.eftigate.entity.ControlEntity;
import com.ingroupe.efti.eftigate.entity.RequestEntity;
import com.ingroupe.efti.eftigate.entity.UilRequestEntity;
import com.ingroupe.efti.eftigate.repository.ControlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes= {ControlRepository.class})
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableJpaRepositories(basePackages = {"com.ingroupe.efti.eftigate.repository"})
@EntityScan("com.ingroupe.efti.eftigate.entity")
class ControlRepositoryTest {

    @Autowired
    private ControlRepository controlRepository;

    @Test
    void shouldFindControlByCriteria(){
        //Arrange
        final RequestEntity firstRequest = new UilRequestEntity();
        firstRequest.setStatus(RequestStatusEnum.IN_PROGRESS);
        final RequestEntity secondRequest = new UilRequestEntity();
        secondRequest.setStatus(RequestStatusEnum.RECEIVED);
        final ControlEntity firstControl = ControlEntity.builder().requestUuid("67fe38bd-6bf7-4b06-b20e-206264bd639c").status(StatusEnum.PENDING).requests(List.of(firstRequest)).build();
        firstRequest.setControl(firstControl);
        final ControlEntity firstSavedControl = controlRepository.save(firstControl);
        final ControlEntity secondControl = ControlEntity.builder().requestUuid("23fe38bd-6bf7-4b06-b20e-206264bd66c").status(StatusEnum.ERROR).requests(List.of(secondRequest)).build();
        secondRequest.setControl(secondControl);
        controlRepository.save(secondControl);

        //Act
        final List<ControlEntity> controls = controlRepository.findByCriteria("67fe38bd-6bf7-4b06-b20e-206264bd639c", RequestStatusEnum.IN_PROGRESS);

        //Assert
        assertThat(controls).containsExactlyInAnyOrder(firstSavedControl);
    }

}
