package eu.efti.identifiersregistry.service;

import eu.efti.commons.dto.IdentifiersDto;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftilogger.service.AuditRegistryLogService;
import eu.efti.identifiersregistry.IdentifiersMapper;
import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
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
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentifiersService {

    private final IdentifiersRepository repository;
    private final IdentifiersMapper mapper;
    private final AuditRegistryLogService logService;
    private final SerializeUtils serializeUtils;

    @Value("${gate.owner}")
    private String gateOwner;
    @Value("${gate.country}")
    private String gateCountry;

    public void createOrUpdate(final SaveIdentifiersRequestWrapper identifiersDto) {
        final String bodyBase64 = serializeUtils.mapObjectToBase64String(identifiersDto);
        final SaveIdentifiersRequest identifiers = identifiersDto.getSaveIdentifiersRequest();

        final Optional<Consignment> entityOptional = repository.findByUil(gateOwner,
                identifiers.getDatasetId(), identifiersDto.getPlatformId());

        Consignment consignment = mapper.dtoToEntity(identifiers);
        consignment.setGateId(gateOwner);
        consignment.setPlatformId(identifiersDto.getPlatformId());
        consignment.setDatasetId(identifiers.getDatasetId());

        if (entityOptional.isPresent()) {
            consignment.setId(entityOptional.get().getId());
            log.info("updating Consignment for uuid {}", consignment.getId());
        } else {
            log.info("creating new entry for dataset id {}", identifiers.getDatasetId());
        }
        this.save(consignment);
        logService.log(identifiersDto, gateOwner, gateCountry, bodyBase64);
    }

    public void disable(final IdentifiersDto identifiersDto) {
        identifiersDto.setDisabled(true);
        this.save(identifiersDto);
    }

    public boolean existByUIL(final String dataUuid, final String gate, final String platform) {
        return this.repository.findByUil(gate, dataUuid, platform).isPresent();
    }

    @Transactional("identifiersTransactionManager")
    public List<IdentifiersDto> search(final SearchWithIdentifiersRequestDto identifiersRequestDto) {
        return mapper.entityListToDtoList(this.repository.searchByCriteria(identifiersRequestDto));
    }

    private IdentifiersDto save(final IdentifiersDto identifiersDto) {
        return mapper.entityToDto(repository.save(mapper.dtoToEntity(identifiersDto)));
    }

    private IdentifiersDto save(final Consignment consignment) {
        return mapper.entityToDto(repository.save(consignment));
    }
}
