package eu.efti.identifiersregistry.service;

import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftilogger.model.ComponentType;
import eu.efti.eftilogger.service.AuditRegistryLogService;
import eu.efti.identifiersregistry.IdentifiersMapper;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.repository.IdentifiersRepository;
import eu.efti.v1.edelivery.SaveIdentifiersRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentifiersService {

    public static final String FTI_005 = "fti005";
    public static final String FTI_004 = "fti004";

    private final IdentifiersRepository identifiersRepository;
    private final IdentifiersMapper mapper;
    private final AuditRegistryLogService auditRegistryLogService;
    private final SerializeUtils serializeUtils;

    @Value("${gate.owner}")
    private String gateOwner;
    @Value("${gate.country}")
    private String gateCountry;

    public void createOrUpdate(final SaveIdentifiersRequestWrapper identifiersDto) {
        final String bodyBase64 = serializeUtils.mapObjectToBase64String(identifiersDto);

        //log fti004
        auditRegistryLogService.log(identifiersDto, gateOwner, gateCountry, ComponentType.PLATFORM, ComponentType.GATE, gateOwner, identifiersDto.getPlatformId(), bodyBase64, FTI_004);
        final SaveIdentifiersRequest identifiers = identifiersDto.getSaveIdentifiersRequest();

        final Optional<Consignment> entityOptional = identifiersRepository.findByUil(gateOwner,
                identifiers.getDatasetId(), identifiersDto.getPlatformId());

        Consignment consignment = mapper.eDeliveryToEntity(identifiers);
        consignment.setGateId(gateOwner);
        consignment.setPlatformId(identifiersDto.getPlatformId());
        consignment.setDatasetId(identifiers.getDatasetId());

        if (entityOptional.isPresent()) {
            consignment.setId(entityOptional.get().getId());
            log.info("updating Consignment for uuid {}", consignment.getId());
        } else {
            log.info("creating new entry for dataset id {}", identifiers.getDatasetId());
        }
        identifiersRepository.save(consignment);
        //log fti005
        auditRegistryLogService.log(identifiersDto, gateOwner, gateCountry, ComponentType.GATE, ComponentType.GATE, null, gateOwner, bodyBase64, FTI_005);
    }

    public ConsignmentDto findByUIL(final String dataUuid, final String gate, final String platform) {
        Optional<Consignment> consignment = this.identifiersRepository.findByUil(gate, dataUuid, platform);
        return consignment.map(mapper::entityToDto).orElse(null);
    }

    @Transactional("identifiersTransactionManager")
    public List<ConsignmentDto> search(final SearchWithIdentifiersRequestDto identifiersRequestDto) {
        return mapper.entityToDto(this.identifiersRepository.searchByCriteria(identifiersRequestDto));
    }
}
