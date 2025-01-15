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
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {IdentifiersRepository.class})
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableJpaRepositories(basePackages = {"eu.efti.identifiersregistry.repository"})
@EntityScan("eu.efti.identifiersregistry.entity")
class IdentifiersQueryTest {
    private static Logger logger = LoggerFactory.getLogger(IdentifiersQueryTest.class);

    private static final String IDENTIFIER_QUERY_TEST_CASES_RESOURCE_PATH = "/identifier-query-test-cases.xml";

    @Autowired
    private IdentifiersRepository identifiersRepository;

    private final IdentifiersMapper identifiersMapper = new IdentifiersMapper(new ModelMapper());

    public record TestCase(eu.efti.v1.identifier_query_test_cases.TestCase testCaseSpec, List<Dataset> datasetSpec) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("TestCase(");

            IdentifierQuery query = testCaseSpec.getQuery();
            sb.append("query=[").append(Stream.of(
                    query.getIdentifier().getValue(),
                    StringUtils.trimToNull(String.join(", ", query.getIdentifier().getType().stream().map(IdentifierType::value).toList())),
                    query.getModeCode(),
                    query.getRegistrationCountryCode(),
                    Optional.ofNullable(query.isDangerousGoodsIndicator()).map(Object::toString).orElse(null)
            ).filter(Objects::nonNull).collect(Collectors.joining(", ")));
            sb.append("], ");

            sb.append("result=[");
            sb.append(String.join(", ", testCaseSpec.getResult()));
            sb.append("]");

            sb.append(")");
            return sb.toString();
        }
    }

    public static Stream<TestCase> readTestCases() {
        final String xml = readXml();
        final IdentifierQueryTestCases spec = unmarshal(xml);
        return spec.getDataGroup().stream().flatMap((it) -> it.getTestCase().stream().map((tc) -> new TestCase(tc, it.getDataset())));
    }

    private static String readXml() {
        final String xml;
        try {
            xml = Files.readString(Paths.get(requireNonNull(IdentifiersQueryTest.class.getResource(IDENTIFIER_QUERY_TEST_CASES_RESOURCE_PATH)).getPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return xml;
    }

    @ParameterizedTest
    @MethodSource("readTestCases")
    public void searchByCriteriaConformsToReferenceTestCases(TestCase testCase) {
        int seed = LocalDate.now().getDayOfMonth() % 4;
        final Random random = new Random(seed);
        logger.info("Using random seed {}", seed);

        testCase.datasetSpec.forEach(dataset -> {
            var entity = toEntity(dataset.getConsignment(), dataset.getId(), random);
            identifiersRepository.save(entity);
        });

        var query = toQuery(testCase.testCaseSpec.getQuery());
        var expectedDatasetIds = new HashSet<>(testCase.testCaseSpec.getResult());

        var results = identifiersRepository.searchByCriteria(query.build());
        var resultIds = results.stream().map(Consignment::getDatasetId).collect(Collectors.toSet());

        assertEquals(expectedDatasetIds, resultIds);
    }

    private static SearchWithIdentifiersRequestDto.SearchWithIdentifiersRequestDtoBuilder toQuery(IdentifierQuery querySpec) {
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
        return query;
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

    /**
     * Creates an entity from test case consignment. Populates random values for those fields that are required by our
     * implementation but omitted in test case specs.
     */
    private Consignment toEntity(SupplyChainConsignment sourceConsignment, String datasetId, Random random) {
        sourceConsignment.getMainCarriageTransportMovement().forEach(tm -> {
            if (tm.isDangerousGoodsIndicator() == null) {
                tm.setDangerousGoodsIndicator(random.nextBoolean());
            }
        });
        Consignment consignment = identifiersMapper.eDeliverySupplyToEntity(sourceConsignment);

        consignment.setGateId("france");
        consignment.setPlatformId("acme");
        consignment.setDatasetId(datasetId);

        return consignment;
    }
}
