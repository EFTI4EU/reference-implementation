package eu.efti.eftigate.service.repository;

import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.entity.UilRequestEntity;
import eu.efti.eftigate.repository.ControlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ControlRepository.class})
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableJpaRepositories(basePackages = {"eu.efti.eftigate.repository"})
@EntityScan("eu.efti.eftigate.entity")
class ControlRepositoryTest {

    @Autowired
    private ControlRepository controlRepository;

    @Test
    void shouldFindControlByCriteria() {
        //Arrange
        final RequestEntity firstRequest = new UilRequestEntity();
        firstRequest.setStatus(RequestStatusEnum.IN_PROGRESS);
        final RequestEntity secondRequest = new UilRequestEntity();
        secondRequest.setStatus(RequestStatusEnum.RECEIVED);
        final ControlEntity firstControl = ControlEntity.builder().requestId("67fe38bd-6bf7-4b06-b20e-206264bd639c").status(StatusEnum.PENDING).requests(List.of(firstRequest)).build();
        firstRequest.setControl(firstControl);
        final ControlEntity firstSavedControl = controlRepository.save(firstControl);
        final ControlEntity secondControl = ControlEntity.builder().requestId("23fe38bd-6bf7-4b06-b20e-206264bd66c").status(StatusEnum.ERROR).requests(List.of(secondRequest)).build();
        secondRequest.setControl(secondControl);
        controlRepository.save(secondControl);

        //Act
        final List<ControlEntity> controls = controlRepository.findByCriteria("67fe38bd-6bf7-4b06-b20e-206264bd639c", RequestStatusEnum.IN_PROGRESS);

        //Assert
        assertThat(controls).containsExactlyInAnyOrder(firstSavedControl);
    }

    @Test
    void shouldFindControlByCriteriaStatusEnum() {
        //Arrange
        final RequestEntity firstRequest = new UilRequestEntity();
        firstRequest.setStatus(RequestStatusEnum.IN_PROGRESS);
        final RequestEntity secondRequest = new UilRequestEntity();
        secondRequest.setStatus(RequestStatusEnum.RECEIVED);
        final ControlEntity firstControl = ControlEntity.builder().requestId("67fe38bd-6bf7-4b06-b20e-206264bd639c").status(StatusEnum.PENDING).requests(List.of(firstRequest)).build();
        firstControl.setCreatedDate(LocalDateTime.now());
        firstRequest.setControl(firstControl);
        final ControlEntity firstSavedControl = controlRepository.save(firstControl);
        final ControlEntity secondControl = ControlEntity.builder().requestId("23fe38bd-6bf7-4b06-b20e-206264bd66c").status(StatusEnum.ERROR).requests(List.of(secondRequest)).build();
        secondRequest.setControl(secondControl);
        controlRepository.save(secondControl);

        //Act
        final List<ControlEntity> controls = controlRepository.findByCriteria(StatusEnum.PENDING, 0);

        //Assert
        assertThat(controls).containsExactlyInAnyOrder(firstSavedControl);
    }



}
