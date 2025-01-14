package eu.efti.identifiersregistry.service;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.identifiersregistry.IdentifiersMapper;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.repository.IdentifiersRepository;
import eu.efti.v1.consignment.identifier.SupplyChainConsignment;
import eu.efti.v1.edelivery.IdentifierQuery;
import eu.efti.v1.edelivery.IdentifierType;
import eu.efti.v1.identifier_query_test_cases.Dataset;
import eu.efti.v1.identifier_query_test_cases.IdentifierQueryTestCases;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {IdentifiersRepository.class})
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableJpaRepositories(basePackages = {"eu.efti.identifiersregistry.repository"})
@EntityScan("eu.efti.identifiersregistry.entity")
class IdentifiersQueryTest {

    @Autowired
    private IdentifiersRepository identifiersRepository;

    private final IdentifiersMapper identifiersMapper = new IdentifiersMapper(new ModelMapper());

    public record TestCase(eu.efti.v1.identifier_query_test_cases.TestCase testCaseSpec, List<Dataset> datasetSpec) {
    }

    public static Stream<TestCase> readTestCases() {
        final String path = "/home/mikko-suniala/src/efti/reference-implementation/schema/xsd/examples/identifier-query-test-cases.xml";
        final String xml;
        try {
            xml = Files.readString(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final IdentifierQueryTestCases rawTestCases = unmarshal(xml);
        return rawTestCases.getDataGroup().stream().flatMap((it) -> it.getTestCase().stream().map((tc) -> new TestCase(tc, it.getDataset())));
    }

    @ParameterizedTest
    @MethodSource("readTestCases")
    public void searchByCriteriaConformsToReferenceTestCases(TestCase testCase) {
        testCase.datasetSpec.forEach(dataset -> {
            var entity = toEntity(dataset.getConsignment(), dataset.getId());
            identifiersRepository.save(entity);
        });

        var querySpec = testCase.testCaseSpec.getQuery();
        var query = SearchWithIdentifiersRequestDto.builder()
                .identifier(querySpec.getIdentifier().getValue());
        if (querySpec.getIdentifier().getType() != null && !querySpec.getIdentifier().getType().isEmpty()) {
            query.identifierType(querySpec.getIdentifier().getType().stream().map(IdentifierType::value).toList());
        }
        if (querySpec.isDangerousGoodsIndicator() != null) {
            query.dangerousGoodsIndicator(querySpec.isDangerousGoodsIndicator());
        }
        if (querySpec.getModeCode() != null) {
            query.modeCode(querySpec.getModeCode());
        }
        if (querySpec.getRegistrationCountryCode() != null) {
            query.registrationCountryCode(querySpec.getRegistrationCountryCode());
        }

        var results = identifiersRepository.searchByCriteria(query.build());
        var resultIds = results.stream().map(Consignment::getDatasetId).collect(Collectors.toSet());
        var expectedDatasetIds = new HashSet<>(testCase.testCaseSpec.getResult());

        assertEquals(expectedDatasetIds, resultIds);
    }

    private static IdentifierQueryTestCases unmarshal(final String content) {
        try {
            final Unmarshaller unmarshaller = JAXBContext.newInstance(IdentifierQueryTestCases.class).createUnmarshaller();
            StreamSource ss = new StreamSource(new ByteArrayInputStream(content.getBytes()));
            final JAXBElement<IdentifierQueryTestCases> jaxbElement = unmarshaller.unmarshal(ss, IdentifierQueryTestCases.class);
            return jaxbElement.getValue();
        } catch (final JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private Consignment toEntity(SupplyChainConsignment sourceConsignment, String datasetId) {
        Consignment consignment = identifiersMapper.eDeliverySupplyToEntity(sourceConsignment);

        consignment.setGateId("france");
        consignment.setPlatformId("acme");
        consignment.setDatasetId(datasetId);

        return consignment;
    }
}
