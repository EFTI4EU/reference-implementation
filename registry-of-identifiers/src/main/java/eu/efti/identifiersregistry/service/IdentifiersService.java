package eu.efti.identifiersregistry.service;

import eu.efti.commons.dto.IdentifiersDto;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftilogger.service.AuditRegistryLogService;
import eu.efti.identifiersregistry.IdentifiersMapper;
import eu.efti.identifiersregistry.entity.Identifiers;
import eu.efti.identifiersregistry.exception.InvalidIdentifiersException;
import eu.efti.identifiersregistry.repository.IdentifiersRepository;
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

    public static final String FTI_004 = "fti004";

    private final IdentifiersRepository repository;
    private final IdentifiersMapper mapper;
    private final AuditRegistryLogService logService;
    private final SerializeUtils serializeUtils;

    @Value("${gate.owner}")
    private String gateOwner;
    @Value("${gate.country}")
    private String gateCountry;

    public void createOrUpdate(final IdentifiersDto identifiersDto) {
        final String bodyBase64 = serializeUtils.mapObjectToBase64String(identifiersDto);

        this.enrichAndValidate(identifiersDto, bodyBase64);

        final Optional<Identifiers> entityOptional = repository.findByUil(identifiersDto.getEFTIGateUrl(),
                identifiersDto.getEFTIDataUuid(), identifiersDto.getEFTIPlatformUrl());

        if(entityOptional.isPresent()) {
            identifiersDto.setId(entityOptional.get().getId());
            identifiersDto.setIdentifiersUUID(entityOptional.get().getIdentifiersUUID());
            log.info("updating Identifiers for uuid {}", identifiersDto.getIdentifiersUUID());
        } else {
            identifiersDto.setIdentifiersUUID(UUID.randomUUID().toString());
            log.info("creating new entry for uuid {}", identifiersDto.getIdentifiersUUID());
        }
        this.save(identifiersDto);
        logService.log(identifiersDto, gateOwner, gateCountry, bodyBase64, FTI_004);

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

    private void enrichAndValidate(final IdentifiersDto identifiersDto, final String bodyBase64) {
        identifiersDto.setEFTIGateUrl(gateOwner);

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        final Set<ConstraintViolation<IdentifiersDto>> violations = validator.validate(identifiersDto);
        if(!violations.isEmpty()){
            final String message = String.format("rejecting identifiers for uil (gate=%s, uuid=%s, platform=%s) because %s",
                    identifiersDto.getEFTIGateUrl(), identifiersDto.getEFTIPlatformUrl(), identifiersDto.getEFTIPlatformUrl(), violations);
            log.error(message);

            logService.log(identifiersDto, gateOwner, gateCountry, bodyBase64, violations.iterator().next().getMessage());
            throw new InvalidIdentifiersException(message);
        }
    }

    private IdentifiersDto save(final IdentifiersDto identifiersDto) {
        return mapper.entityToDto(repository.save(mapper.dtoToEntity(identifiersDto)));
    }
}
