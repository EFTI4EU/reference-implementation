package eu.efti.eftigate.testsupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract public class RestIntegrationTest extends IntegrationTest {
    @Autowired
    protected RestApiCallerFactory restApiCallerFactory;
}
