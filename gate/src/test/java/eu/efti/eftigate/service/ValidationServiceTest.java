package eu.efti.eftigate.service;

import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.service.request.ValidationService;
import eu.efti.v1.edelivery.Request;
import eu.efti.v1.edelivery.Response;
import jakarta.validation.constraints.AssertTrue;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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
    void isRequestValidatorValidTest() {
        Request request = new Request();
        request.setRequestId("42");

        boolean result = this.validationService.isRequestValidator(request);

        assertTrue(result);
    }

    @Test
    void isRequestValidatorNotValidTest() {
        Request request = new Request();

        boolean result = this.validationService.isRequestValidator(request);

        assertFalse(result);
    }

    @Test
    void isResponseValidorValidTest() {
        Response response = new Response();
        response.setRequestId("42");

        boolean result = this.validationService.isResponseValidator(response);

        assertTrue(result);
    }

    @Test
    void isResponseValidorNotValidTest() {
        Response response = new Response();

        boolean result = this.validationService.isResponseValidator(response);

        assertFalse(result);
    }
}
