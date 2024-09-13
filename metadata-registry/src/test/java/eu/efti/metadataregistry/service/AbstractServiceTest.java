package eu.efti.metadataregistry.service;

import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftilogger.service.AuditRegistryLogService;
import eu.efti.metadataregistry.MetadataMapper;
import org.mockito.Mock;
import org.modelmapper.ModelMapper;

public abstract class AbstractServiceTest {

    public final MetadataMapper mapperUtils = new MetadataMapper(createModelMapper());
    @Mock
    SerializeUtils serializeUtils;
    @Mock
    AuditRegistryLogService auditRegistryLogService;

    private ModelMapper createModelMapper() {
        return new ModelMapper();
    }
}
