package eu.efti.eftigate.service.request;

import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.eftigate.entity.RequestEntity;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("rawtypes")
class RequestServiceFactoryTest {

    @Mock
    private IdentifiersRequestService identifiersRequestService;

    @Mock
    private UilRequestService uilRequestService;

    @Mock
    private NotesRequestService notesRequestService;

    @InjectMocks
    private RequestServiceFactory requestServiceFactory;

    @Spy
    protected List<RequestService<? extends RequestEntity>> requestServices = new ArrayList<>();

    @BeforeEach
    void init() {

        MockitoAnnotations.openMocks(this);

        requestServices.add(identifiersRequestService);
        requestServices.add(uilRequestService);
        requestServices.add(notesRequestService);

        Mockito.when(identifiersRequestService.supports(RequestType.IDENTIFIER.name())).thenReturn(true);
        Mockito.when(uilRequestService.supports(RequestTypeEnum.EXTERNAL_UIL_SEARCH)).thenReturn(true);

    }

    @Test
    void testGetRequestServiceByRequestTypeEnum() {
        RequestService requestServiceByRequestType = requestServiceFactory.getRequestServiceByRequestType(RequestTypeEnum.EXTERNAL_UIL_SEARCH);
        Assertions.assertThat(requestServiceByRequestType).isInstanceOf(UilRequestService.class);
    }

    @Test
    void testGetRequestServiceByRequestType() {
        RequestService requestServiceByRequestType = requestServiceFactory.getRequestServiceByRequestType(RequestType.IDENTIFIER.name());
        Assertions.assertThat(requestServiceByRequestType).isInstanceOf(IdentifiersRequestService.class);
    }

}
