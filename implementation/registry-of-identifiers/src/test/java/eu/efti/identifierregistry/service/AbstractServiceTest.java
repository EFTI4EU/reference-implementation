package eu.efti.identifierregistry.service;

import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftilogger.service.AuditRegistryLogService;
import eu.efti.identifierregistry.IdentifiersMapper;
import org.mockito.Mock;
import org.modelmapper.ModelMapper;

public abstract class AbstractServiceTest {

    public final IdentifiersMapper mapperUtils = new IdentifiersMapper(createModelMapper());
    @Mock
    SerializeUtils serializeUtils;
    @Mock
    AuditRegistryLogService auditRegistryLogService;

    private ModelMapper createModelMapper() {
        return new ModelMapper();
    }
}
