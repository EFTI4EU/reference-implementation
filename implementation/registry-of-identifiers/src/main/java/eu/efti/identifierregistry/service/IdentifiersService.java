package eu.efti.identifierregistry.service;

import eu.efti.commons.dto.ConsignmentIdentifiersDTO;
import eu.efti.commons.dto.ConsignmentIdentifiersRequestDto;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftilogger.service.AuditRegistryLogService;
import eu.efti.identifierregistry.IdentifiersMapper;
import eu.efti.identifierregistry.entity.Consignment;
import eu.efti.identifierregistry.exception.InvalidIdentifiersException;
import eu.efti.identifierregistry.repository.IdentifiersRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
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

    public void createOrUpdate(final ConsignmentIdentifiersDTO metadataDto) {
        final String bodyBase64 = serializeUtils.mapObjectToBase64String(metadataDto);

        this.enrichAndValidate(metadataDto, bodyBase64);

        final Optional<Consignment> entityOptional = repository.findByUil(metadataDto.getEFTIGateUrl(),
                metadataDto.getEFTIDataUuid(), metadataDto.getEFTIPlatformUrl());

        if(entityOptional.isPresent()) {
            metadataDto.setId(entityOptional.get().getId());
            metadataDto.setConsignmentUUID(entityOptional.get().getConsignmentUUID());
            log.info("updating metadata for uuid {}", metadataDto.getConsignmentUUID());
        } else {
            metadataDto.setConsignmentUUID(UUID.randomUUID().toString());
            log.info("creating new entry for uuid {}", metadataDto.getConsignmentUUID());
        }
        this.save(metadataDto);
        logService.log(metadataDto, gateOwner, gateCountry, bodyBase64);
    }

    public void disable(final ConsignmentIdentifiersDTO metadataDto) {
        metadataDto.setDisabled(true);
        this.save(metadataDto);
    }

    public boolean existByUIL(final String dataUuid, final String gate, final String platform) {
        return this.repository.findByUil(gate, dataUuid, platform).isPresent();
    }

    @Transactional("identifiersTransactionManager")
    public List<ConsignmentIdentifiersDTO> search(final ConsignmentIdentifiersRequestDto consignmentIdentifiersRequestDto) {
        return mapper.entityListToDtoList(this.repository.searchByCriteria(consignmentIdentifiersRequestDto));
    }

    private void enrichAndValidate(final ConsignmentIdentifiersDTO metadataDto, final String bodyBase64) {
        metadataDto.setEFTIGateUrl(gateOwner);

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        final Set<ConstraintViolation<ConsignmentIdentifiersDTO>> violations = validator.validate(metadataDto);
        if(!violations.isEmpty()){
            final String message = String.format("rejecting metadata for uil (gate=%s, uuid=%s, platform=%s) because %s",
                    metadataDto.getEFTIGateUrl(), metadataDto.getEFTIPlatformUrl(), metadataDto.getEFTIPlatformUrl(), violations);
            log.error(message);

            logService.log(metadataDto, gateOwner, gateCountry, bodyBase64, violations.iterator().next().getMessage());
            throw new InvalidIdentifiersException(message);
        }
    }

    private ConsignmentIdentifiersDTO save(final ConsignmentIdentifiersDTO metadataDto) {
        return mapper.entityToDto(repository.save(mapper.dtoToEntity(metadataDto)));
    }
}
