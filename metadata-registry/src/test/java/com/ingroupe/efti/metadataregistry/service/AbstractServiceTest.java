package com.ingroupe.efti.metadataregistry.service;

import com.ingroupe.efti.commons.utils.SerializeUtils;
import com.ingroupe.efti.eftilogger.service.AuditRegistryLogService;
import com.ingroupe.efti.metadataregistry.MetadataMapper;
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
