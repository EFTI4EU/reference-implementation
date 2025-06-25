package eu.efti.identifiersregistry.service;

import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.enums.RegistryType;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftilogger.model.ComponentType;
import eu.efti.eftilogger.service.AuditRegistryLogService;
import eu.efti.eftilogger.service.ReportingRegistryLogService;
import eu.efti.identifiersregistry.IdentifiersMapper;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.repository.IdentifiersRepository;
import eu.efti.v1.consignment.identifier.SupplyChainConsignment;
import eu.efti.v1.edelivery.SaveIdentifiersRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentifiersService {

    public static final String FTI_005 = "fti005";
    public static final String FTI_004 = "fti004";
    public static final String THREE_MODE_CODE = "3";

    private final IdentifiersRepository identifiersRepository;
    private final IdentifiersMapper mapper;
    private final AuditRegistryLogService auditRegistryLogService;
    private final SerializeUtils serializeUtils;
    private final ReportingRegistryLogService reportingRegistryLogService;

    @Value("${gate.owner}")
    private String gateOwner;
    @Value("${gate.country}")
    private String gateCountry;
    @Value("${batch.identifier.deactivation-delay.three:12}")
    private int modeCodeThreeMaxDayPassed;
    @Value("${batch.identifier.deactivation-delay.other:0}")
    private int modeCodeOtherMaxDayPassed;
    @Value("${batch.identifier.deactivation-delay.null-delivery-date:90}")
    private int nullDeliveryDateMaxDayPassed;
    @Value("${batch.identifier.activated:false}")
    private boolean batchIdentifiersActivated;


    @Transactional("identifiersTransactionManager")
    public int deleteOldConsignment() {
        try {
            return identifiersRepository.deleteAllDisabledConsignment();
        } catch (Exception e) {
            log.error("Error when try to delete old Consignment in database: ", e);
        }
        return 0;
    }

    public void createOrUpdate(final SaveIdentifiersRequestWrapper identifiersDto) {
        final String bodyBase64 = serializeUtils.mapObjectToBase64String(identifiersDto);

        //log fti004
        auditRegistryLogService.log(identifiersDto, gateOwner, gateCountry, ComponentType.PLATFORM, ComponentType.GATE, identifiersDto.getPlatformId(), gateOwner, bodyBase64, FTI_004);
        final SaveIdentifiersRequest identifiers = identifiersDto.getSaveIdentifiersRequest();

        final Optional<Consignment> entityOptional = identifiersRepository.findByUil(gateOwner,
                identifiers.getDatasetId(), identifiersDto.getPlatformId());

        Consignment consignment = mapper.eDeliveryToEntity(identifiers);
        consignment.setGateId(gateOwner);
        consignment.setPlatformId(identifiersDto.getPlatformId());
        consignment.setDatasetId(identifiers.getDatasetId());
        consignment = setDisabledDate(consignment);
        if (entityOptional.isPresent()) {
            consignment.setId(entityOptional.get().getId());
            log.info("updating Consignment for uuid {}", consignment.getId());
        } else {
            log.info("creating new entry for dataset id {}", identifiers.getDatasetId());
        }
        identifiersRepository.save(consignment);
        //log reporting Upload Identifiiers
        reportingRegistryLogService.logRegistryRequest(gateOwner, gateCountry, ComponentType.PLATFORM, identifiersDto.getPlatformId(), gateCountry, identifiersDto, entityOptional.isPresent() ? RegistryType.UPDATE : RegistryType.UPLOAD);
        //log fti005
        auditRegistryLogService.log(identifiersDto, gateOwner, gateCountry, ComponentType.GATE, ComponentType.GATE, null, gateOwner, bodyBase64, FTI_005);
    }

    public boolean consignmentExistsByUIL(final String datasetId, final String gate, final String platform) {
        return this.identifiersRepository.findActiveByUil(gate, datasetId, platform).isPresent();
    }

    public void createOrUpdateConsignment(final String body, final String datasetId, final String platformId) {
        SupplyChainConsignment consignment = serializeUtils.mapXmlStringToJaxbObject(body, SupplyChainConsignment.class);
        SaveIdentifiersRequest saveIdentifiersRequest = new SaveIdentifiersRequest();
        saveIdentifiersRequest.setDatasetId(datasetId);
        saveIdentifiersRequest.setConsignment(consignment);
        this.createOrUpdate(new SaveIdentifiersRequestWrapper(platformId, saveIdentifiersRequest));
    }

    @Transactional("identifiersTransactionManager")
    public List<ConsignmentDto> search(final SearchWithIdentifiersRequestDto identifiersRequestDto) {
        return mapper.entityToDto(this.identifiersRepository.searchByCriteria(identifiersRequestDto, batchIdentifiersActivated));
    }

    public Consignment setDisabledDate(final Consignment consignment) {
        final List<Integer> resultList = new java.util.ArrayList<>(List.of());
        consignment.getMainCarriageTransportMovements().forEach(mainCarriageTransportMovement -> {
            if (THREE_MODE_CODE.equals(mainCarriageTransportMovement.getModeCode())) {
                resultList.add(deliveryDateChecker(consignment, modeCodeThreeMaxDayPassed));
            } else {
                resultList.add(deliveryDateChecker(consignment, modeCodeOtherMaxDayPassed));
            }
        });
        return updateCheckerConsignment(consignment, resultList);
    }

    private Consignment updateCheckerConsignment(final Consignment consignment, final List<Integer> resultList) {
        final int maxResult = Collections.max(resultList);
        OffsetDateTime finalDate;
        if (maxResult == nullDeliveryDateMaxDayPassed) {
            finalDate = getOffsetDateTimeForMaxDayPassed(consignment);
        } else {
            finalDate = consignment.getDeliveryEventActualOccurrenceDatetime();
        }
        consignment.setDisabledDate(finalDate.plusDays(maxResult));
        return consignment;
    }

    private OffsetDateTime getOffsetDateTimeForMaxDayPassed(final Consignment consignment) {
        OffsetDateTime finalDate;
        if (consignment.getCarrierAcceptanceDatetime() != null) {
            finalDate = consignment.getCarrierAcceptanceDatetime();
        } else {
            if (consignment.getCreatedDate() != null) {
                finalDate = consignment.getCreatedDate();
            } else {
                finalDate = OffsetDateTime.now();
            }
        }
        return finalDate;
    }

    private int deliveryDateChecker(final Consignment consignment, final int maxDif) {
        if (consignment.getDeliveryEventActualOccurrenceDatetime() != null) {
            return maxDif;
        } else {
            return nullDeliveryDateMaxDayPassed;
        }
    }
}
