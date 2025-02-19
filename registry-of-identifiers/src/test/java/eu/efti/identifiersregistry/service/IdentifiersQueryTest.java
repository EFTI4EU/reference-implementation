package eu.efti.identifiersregistry.service;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.identifiersregistry.IdentifiersMapper;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.repository.IdentifiersRepository;
import eu.efti.v1.codes.CountryCode;
import eu.efti.v1.consignment.identifier.SupplyChainConsignment;
import eu.efti.v1.consignment.identifier.TradeCountry;
import eu.efti.v1.edelivery.IdentifierQuery;
import eu.efti.v1.edelivery.IdentifierType;
import eu.efti.v1.identifier_query_test_cases.IdentifierQueryTestCases;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.io.IOUtils;
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
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {IdentifiersRepository.class})
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableJpaRepositories(basePackages = {"eu.efti.identifiersregistry.repository"})
@EntityScan("eu.efti.identifiersregistry.entity")
class IdentifiersQueryTest {
    private static final Logger logger = LoggerFactory.getLogger(IdentifiersQueryTest.class);

    private static final String IDENTIFIER_QUERY_TEST_CASES_RESOURCE_PATH = "/identifier-query-test-cases.xml";

    @Autowired
    private IdentifiersRepository identifiersRepository;

    private final IdentifiersMapper identifiersMapper = new IdentifiersMapper(new ModelMapper());

    public record TestCase(String description,
                           eu.efti.v1.identifier_query_test_cases.TestCase testCaseSpec,
                           List<eu.efti.v1.identifier_query_test_cases.Dataset> datasetSpec) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("TestCase(");
            sb.append("'%s', ".formatted(description));

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
        return spec.getDataGroup().stream().flatMap(dg -> dg.getTestCase().stream().map(tc -> new TestCase(dg.getDescription(), tc, dg.getDataset())));
    }

    private static String readXml() {

        try (final InputStream inputStream = IdentifiersQueryTest.class.getResourceAsStream(IDENTIFIER_QUERY_TEST_CASES_RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new RuntimeException(String.format("file %s not found", IDENTIFIER_QUERY_TEST_CASES_RESOURCE_PATH));
            }
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @MethodSource("readTestCases")
    void searchByCriteriaConformsToReferenceTestCases(final TestCase testCase) {
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

    private static SearchWithIdentifiersRequestDto.SearchWithIdentifiersRequestDtoBuilder toQuery(final IdentifierQuery querySpec) {
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
    private Consignment toEntity(final SupplyChainConsignment sourceConsignment, final String datasetId, final Random random) {
        sourceConsignment.getMainCarriageTransportMovement().forEach(tm -> {
            tm.setDangerousGoodsIndicator(random.nextBoolean());
        });
        IntStream.range(0, sourceConsignment.getUsedTransportEquipment().size()).forEach(uteIndex -> {
            var ute = sourceConsignment.getUsedTransportEquipment().get(uteIndex);
            if (ute.getSequenceNumber() == null) {
                ute.setSequenceNumber(BigInteger.valueOf(uteIndex));
            }
            if (ute.getRegistrationCountry() == null) {
                var tc = new TradeCountry();
                var cc = CountryCode.values()[random.nextInt(CountryCode.values().length)];
                tc.setCode(cc);
                ute.setRegistrationCountry(tc);
            }

            IntStream.range(0, ute.getCarriedTransportEquipment().size()).forEach(cteIndex -> {
                var cte = ute.getCarriedTransportEquipment().get(cteIndex);
                cte.setSequenceNumber(BigInteger.valueOf(cteIndex));
            });
        });

        Consignment consignment = identifiersMapper.eDeliverySupplyToEntity(sourceConsignment);

        consignment.setGateId("france");
        consignment.setPlatformId("acme");
        consignment.setDatasetId(datasetId);

        return consignment;
    }
}
