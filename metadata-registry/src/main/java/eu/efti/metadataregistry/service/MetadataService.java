package eu.efti.metadataregistry.service;

import eu.efti.commons.dto.MetadataDto;
import eu.efti.commons.dto.MetadataRequestDto;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftilogger.service.AuditRegistryLogService;
import eu.efti.metadataregistry.MetadataMapper;
import eu.efti.metadataregistry.entity.MetadataEntity;
import eu.efti.metadataregistry.exception.InvalidMetadataException;
import eu.efti.metadataregistry.repository.MetadataRepository;
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
public class MetadataService {

    public static final String FTI_004 = "fti004";

    private final MetadataRepository repository;
    private final MetadataMapper mapper;
    private final AuditRegistryLogService logService;
    private final SerializeUtils serializeUtils;

    @Value("${gate.owner}")
    private String gateOwner;
    @Value("${gate.country}")
    private String gateCountry;

    public void createOrUpdate(final MetadataDto metadataDto) {
        final String bodyBase64 = serializeUtils.mapObjectToBase64String(metadataDto);

        this.enrichAndValidate(metadataDto, bodyBase64);

        final Optional<MetadataEntity> entityOptional = repository.findByUil(metadataDto.getEFTIGateUrl(),
                metadataDto.getEFTIDataUuid(), metadataDto.getEFTIPlatformUrl());

        if(entityOptional.isPresent()) {
            metadataDto.setId(entityOptional.get().getId());
            metadataDto.setMetadataUUID(entityOptional.get().getMetadataUUID());
            log.info("updating metadata for uuid {}", metadataDto.getMetadataUUID());
        } else {
            metadataDto.setMetadataUUID(UUID.randomUUID().toString());
            log.info("creating new entry for uuid {}", metadataDto.getMetadataUUID());
        }
        this.save(metadataDto);
        logService.log(metadataDto, gateOwner, gateCountry, bodyBase64, FTI_004);
    }

    public void disable(final MetadataDto metadataDto) {
        metadataDto.setDisabled(true);
        this.save(metadataDto);
    }

    public boolean existByUIL(final String dataUuid, final String gate, final String platform) {
        return this.repository.findByUil(gate, dataUuid, platform).isPresent();
    }

    @Transactional("metadataTransactionManager")
    public List<MetadataDto> search(final MetadataRequestDto metadataRequestDto) {
        return mapper.entityListToDtoList(this.repository.searchByCriteria(metadataRequestDto));
    }

    private void enrichAndValidate(final MetadataDto metadataDto, final String bodyBase64) {
        metadataDto.setEFTIGateUrl(gateOwner);

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        final Set<ConstraintViolation<MetadataDto>> violations = validator.validate(metadataDto);
        if(!violations.isEmpty()){
            final String message = String.format("rejecting metadata for uil (gate=%s, uuid=%s, platform=%s) because %s",
                    metadataDto.getEFTIGateUrl(), metadataDto.getEFTIPlatformUrl(), metadataDto.getEFTIPlatformUrl(), violations);
            log.error(message);

            logService.log(metadataDto, gateOwner, gateCountry, bodyBase64, violations.iterator().next().getMessage());
            throw new InvalidMetadataException(message);
        }
    }

    private MetadataDto save(final MetadataDto metadataDto) {
        return mapper.entityToDto(repository.save(mapper.dtoToEntity(metadataDto)));
    }
}
