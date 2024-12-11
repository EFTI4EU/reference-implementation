package eu.efti.eftigate.service;

import eu.efti.eftigate.service.request.ValidationService;
import eu.efti.v1.edelivery.Request;
import eu.efti.v1.edelivery.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void before() {
        validationService = new ValidationService();
    }

    @Test
    void isRequestValidTest() {
        Request request = new Request();
        request.setRequestId("42");

        boolean result = this.validationService.isRequestValid(request);

        assertTrue(result);
    }

    @Test
    void isRequestNotValidTest() {
        Request request = new Request();

        boolean result = this.validationService.isRequestValid(request);

        assertFalse(result);
    }

    @Test
    void isResponseValidTest() {
        Response response = new Response();
        response.setRequestId("42");

        boolean result = this.validationService.isResponseValid(response);

        assertTrue(result);
    }

    @Test
    void isResponseNotValidTest() {
        Response response = new Response();

        boolean result = this.validationService.isResponseValid(response);

        assertFalse(result);
    }
}
