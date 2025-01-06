package eu.efti.platformgatesimulator.service;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.UilDto;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.edeliveryapconnector.constant.EDeliveryStatus;
import eu.efti.edeliveryapconnector.dto.ApConfigDto;
import eu.efti.edeliveryapconnector.dto.ApRequestDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.exception.SendRequestException;
import eu.efti.edeliveryapconnector.service.RequestSendingService;
import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import eu.efti.v1.edelivery.Identifier;
import eu.efti.v1.edelivery.IdentifierQuery;
import eu.efti.v1.edelivery.IdentifierResponse;
import eu.efti.v1.edelivery.ObjectFactory;
import eu.efti.v1.edelivery.UIL;
import eu.efti.v1.edelivery.UILQuery;
import eu.efti.v1.edelivery.UILResponse;
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

    public void sendResponseUil(final String requestId, final SupplyChainConsignment consignment) {
        sendResponse(requestId, consignment);
    }

    private void sendResponse(final String requestId, final SupplyChainConsignment consignment) {
        final ApRequestDto apRequestDto = ApRequestDto.builder()
                .requestId(requestId)
                .sender(gateProperties.getOwner())
                .receiver(gateProperties.getGate())
                .body(buildBody(requestId, consignment))
                .apConfig(ApConfigDto.builder()
                        .username(gateProperties.getAp().getUsername())
                        .password(gateProperties.getAp().getPassword())
                        .url(gateProperties.getAp().getUrl())
                        .build())
                .build();
        try {
            requestSendingService.sendRequest(apRequestDto);
        } catch (final SendRequestException e) {
            log.error("SendRequestException received : ", e);
        }
    }

    private String buildBody(final String requestId, final SupplyChainConsignment consignment) {
        final UILResponse uilResponse = new UILResponse();
        if (defineBadOrGoodRequest()) {
            log.info("Good request will be send");
            uilResponse.setRequestId(requestId);
            uilResponse.setDescription(null);
            uilResponse.setStatus(EDeliveryStatus.OK.getCode());
            uilResponse.setConsignment(consignment);
        } else {
            log.info("Bad request will be send");
            uilResponse.setRequestId(requestId);
            uilResponse.setDescription("Not found");
            uilResponse.setStatus(EDeliveryStatus.NOT_FOUND.getCode());
            uilResponse.setConsignment(null);
        }

        final JAXBElement<UILResponse> jaxbElement = objectFactory.createUilResponse(uilResponse);
        return serializeUtils.mapJaxbObjectToXmlString(jaxbElement, UILResponse.class);
    }

    private boolean defineBadOrGoodRequest() {
        if (badRequestPercentage >= 1 || badRequestPercentage <= 0) {
            return true;
        }
        float randFloat = random.nextFloat();
        return randFloat >= badRequestPercentage;
    }

    public void sendRequestUil(final UilDto uilDto) {
        if (isTimerActiveForIdentifierResponse) {
            makeGaussTime();
        }
        requestSendingService.sendRequest(buildApRequestDtoUilQuery(uilDto));
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
        double gauss = (random.nextGaussian() * standardDeviation + average);
        if (gauss < 0) {
            gauss = 0;
        }
        return gauss;
    }

    private ApRequestDto buildApRequestDtoUilQuery(final UilDto uilDto) {
        final String requestId = UUID.randomUUID().toString();
        return ApRequestDto.builder()
                .requestId(requestId)
                .sender(gateProperties.getOwner())
                .receiver(gateProperties.getGate())
                .body(UilQueryString(uilDto, requestId))
                .apConfig(ApConfigDto.builder()
                        .username(gateProperties.getAp().getUsername())
                        .password(gateProperties.getAp().getPassword())
                        .url(gateProperties.getAp().getUrl())
                        .build())
                .build();
    }

    public String UilQueryString(final UilDto uilDto, final String requestId) {
        final UILQuery uilQuery = new UILQuery();
        final UIL uil = new UIL();
        uil.setDatasetId(uilDto.getDatasetId());
        uil.setPlatformId(gateProperties.getOwner());
        uil.setGateId(gateProperties.getGate());
        uilQuery.setUil(uil);
        uilQuery.setRequestId(requestId);
        uilQuery.setSubsetId("full");

        final JAXBElement<UILQuery> jaxBResponse = objectFactory.createUilQuery(uilQuery);
        return serializeUtils.mapJaxbObjectToXmlString(jaxBResponse, UILQuery.class);
    }

    private String queryIdentifierString(final SearchWithIdentifiersRequestDto searchWithIdentifiersRequestDto, final String requestId) {
        final IdentifierQuery identifierQuery = new IdentifierQuery();

        final Identifier identifier = new Identifier();
        identifier.setValue(searchWithIdentifiersRequestDto.getIdentifier());

        identifierQuery.setIdentifier(identifier);
        identifierQuery.setModeCode(searchWithIdentifiersRequestDto.getModeCode());
        identifierQuery.setDangerousGoodsIndicator(searchWithIdentifiersRequestDto.getDangerousGoodsIndicator());
        identifierQuery.setRegistrationCountryCode(searchWithIdentifiersRequestDto.getRegistrationCountryCode());
        identifierQuery.setRequestId(requestId);

        final JAXBElement<IdentifierQuery> jaxBResponse = objectFactory.createIdentifierQuery(identifierQuery);
        return serializeUtils.mapJaxbObjectToXmlString(jaxBResponse, IdentifierQuery.class);
    }

    public ApRequestDto buildApRequestQueryIdentifier(final SearchWithIdentifiersRequestDto searchWithIdentifiersRequestDto) {
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

    public void sendResponseIdentifier(final IdentifierQuery identifierQuery, final NotificationDto notificationDto) {
        final IdentifierResponse identifierResponse = buildIdentifierResponse(identifierQuery);
        if (isTimerActiveForIdentifierResponse) {
            makeGaussTime();
        }
        requestSendingService.sendRequest(buildApRequestDtoIdentifierResponse(identifierResponse, notificationDto));
    }

    private ApRequestDto buildApRequestDtoIdentifierResponse(final IdentifierResponse identifierResponse, final NotificationDto notificationDto) {
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

    private String identifierResponseString(final IdentifierResponse identifierResponse) {
        final JAXBElement<IdentifierResponse> jaxBResponse = objectFactory.createIdentifierResponse(identifierResponse);
        return serializeUtils.mapJaxbObjectToXmlString(jaxBResponse, IdentifierResponse.class);
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
