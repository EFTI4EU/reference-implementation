package eu.efti.platformgatesimulator.service;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.edeliveryapconnector.dto.ApConfigDto;
import eu.efti.edeliveryapconnector.dto.ApRequestDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.service.RequestSendingService;
import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.v1.edelivery.Identifier;
import eu.efti.v1.edelivery.IdentifierQuery;
import eu.efti.v1.edelivery.IdentifierResponse;
import eu.efti.v1.edelivery.ObjectFactory;
import jakarta.xml.bind.JAXBElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

import static java.lang.Thread.sleep;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentifierService {

    @Value("${mock.gaussWaitingTime.average:0}")
    int average;

    @Value("${mock.gaussWaitingTime.standardDeviation:0}")
    int standardDeviation;

    @Value("${mock.gaussWaitingTime.isActiveForIdentifierRequestTimer:false}")
    boolean isActiveForIdentifierRequestTimer;

    @Value("${mock.gaussWaitingTime.isTimerActiveForIdentifierResponse:false}")
    boolean isTimerActiveForIdentifierResponse;

    @Value("${mock.identifierReponseGoodResponse.description:description}")
    String descriptionIdentifierResponse;

    @Value("${mock.identifierReponseGoodResponse.status:200}")
    String statusIdentifierResponse;

    @Value("${mock.identifierReponseBadResponse.description:bad gateway}")
    String descriptionIdentifierBadResponse;

    @Value("${mock.identifierReponseBadResponse.status:502}")
    String statusIdentifierBadResponse;

    @Value("${mock.badRequestPercentage:0}")
    float badRequestPercentage;


    private final RequestSendingService requestSendingService;

    private final GateProperties gateProperties;

    private final ObjectFactory objectFactory = new ObjectFactory();

    private final SerializeUtils serializeUtils;

    private final Random random = new Random();

    private String queryIdentifierString(SearchWithIdentifiersRequestDto searchWithIdentifiersRequestDto, String requestId) {
        IdentifierQuery identifierQuery = new IdentifierQuery();

        Identifier identifier = new Identifier();
        identifier.setValue(searchWithIdentifiersRequestDto.getIdentifier());

        identifierQuery.setIdentifier(identifier);
        identifierQuery.setModeCode(searchWithIdentifiersRequestDto.getModeCode());
        identifierQuery.setDangerousGoodsIndicator(searchWithIdentifiersRequestDto.getDangerousGoodsIndicator());
        identifierQuery.setRegistrationCountryCode(searchWithIdentifiersRequestDto.getRegistrationCountryCode());
        identifierQuery.setRequestId(requestId);

        final JAXBElement<IdentifierQuery> jaxBResponse = objectFactory.createIdentifierQuery(identifierQuery);
        return serializeUtils.mapJaxbObjectToXmlString(jaxBResponse, IdentifierQuery.class);
    }

    public ApRequestDto buildApRequestQueryIdentifier(SearchWithIdentifiersRequestDto searchWithIdentifiersRequestDto) {
        final String requestId = UUID.randomUUID().toString();
        return ApRequestDto.builder()
                .requestId(requestId)
                .sender(gateProperties.getOwner())
                .receiver(gateProperties.getGate())
                .body(queryIdentifierString(searchWithIdentifiersRequestDto, requestId))
                .apConfig(ApConfigDto.builder()
                        .username(gateProperties.getAp().getUsername())
                        .password(gateProperties.getAp().getPassword())
                        .url(gateProperties.getAp().getUrl())
                        .build())
                .build();
    }

    public void sendIdentifierRequest(final SearchWithIdentifiersRequestDto searchWithIdentifiersRequestDto) {
        if (isActiveForIdentifierRequestTimer) {
            makeGaussTime();
        }
        requestSendingService.sendRequest(buildApRequestQueryIdentifier(searchWithIdentifiersRequestDto));
    }

    private void makeGaussTime() {
        try {
            final long gaussWaitingTime = Math.round(callGauss());
            log.info("Platform will wait {} sec before send request", gaussWaitingTime);
            sleep(gaussWaitingTime * 1000);
        } catch (InterruptedException e) {
            log.error("Error when try to call gauss operation !", e);
        }
    }

    private double callGauss() {
        double gauss = (random.nextGaussian()*standardDeviation+average);
        if (gauss < 0 ) {
            gauss = 0;
        }
        return gauss;
    }

    public void sendResponseIdentifier(final IdentifierQuery identifierQuery, final NotificationDto notificationDto) {
        final IdentifierResponse identifierResponse = buildIdentifierResponse(identifierQuery);
        if (isTimerActiveForIdentifierResponse) {
            makeGaussTime();
        }
        requestSendingService.sendRequest(buildApRequestDtoIdentifierResponse(identifierResponse, notificationDto));
    }

    private ApRequestDto buildApRequestDtoIdentifierResponse(IdentifierResponse identifierResponse, NotificationDto notificationDto) {
        final String requestId = UUID.randomUUID().toString();
        return ApRequestDto.builder()
                .requestId(requestId)
                .sender(gateProperties.getOwner())
                .receiver(notificationDto.getContent().getFromPartyId())
                .body(identifierResponseString(identifierResponse))
                .apConfig(ApConfigDto.builder()
                        .username(gateProperties.getAp().getUsername())
                        .password(gateProperties.getAp().getPassword())
                        .url(gateProperties.getAp().getUrl())
                        .build())
                .build();
    }

    private String identifierResponseString(IdentifierResponse identifierResponse) {
        final JAXBElement<IdentifierResponse> jaxBResponse = objectFactory.createIdentifierResponse(identifierResponse);
        return serializeUtils.mapJaxbObjectToXmlString(jaxBResponse, IdentifierResponse.class);
    }

    private boolean defineBadOrGoodRequest() {
        if (badRequestPercentage >= 1 || badRequestPercentage <= 0) {
            return true;
        }
        float randFloat = random.nextFloat();
        return randFloat >= badRequestPercentage;
    }

    private IdentifierResponse buildIdentifierResponse(final IdentifierQuery identifierQuery) {
        final IdentifierResponse identifierResponse = new IdentifierResponse();

        if (defineBadOrGoodRequest()) {
            log.info("Good request will be send");
            identifierResponse.setRequestId(identifierQuery.getRequestId());
            identifierResponse.setDescription(descriptionIdentifierResponse);
            identifierResponse.setStatus(statusIdentifierResponse);
        } else {
            log.info("Bad request will be sent");
            identifierResponse.setRequestId(identifierQuery.getRequestId());
            identifierResponse.setDescription(descriptionIdentifierBadResponse);
            identifierResponse.setStatus(statusIdentifierBadResponse);
        }
        return identifierResponse;
    }
}
