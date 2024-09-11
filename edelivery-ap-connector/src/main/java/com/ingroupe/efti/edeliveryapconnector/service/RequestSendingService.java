package com.ingroupe.efti.edeliveryapconnector.service;

import com.ingroupe.efti.commons.enums.EDeliveryAction;
import com.ingroupe.efti.edeliveryapconnector.dto.ApRequestDto;
import com.ingroupe.efti.edeliveryapconnector.exception.SendRequestException;
import com.sun.xml.ws.client.ClientTransportException;
import com.sun.xml.ws.wsdl.parser.InaccessibleWSDLException;
import eu.domibus.plugin.ws.generated.SubmitMessageFault;
import eu.domibus.plugin.ws.generated.body.LargePayloadType;
import eu.domibus.plugin.ws.generated.body.SubmitRequest;
import eu.domibus.plugin.ws.generated.body.SubmitResponse;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.CollaborationInfo;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.From;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageProperties;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartInfo;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartProperties;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyId;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyInfo;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PayloadInfo;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.ProcessingType;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Property;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Service;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.To;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.net.MalformedURLException;

import static com.ingroupe.efti.edeliveryapconnector.constant.ApConstant.FINAL_RECIPIENT_PROPERTY_KEY;
import static com.ingroupe.efti.edeliveryapconnector.constant.ApConstant.FINAL_RECIPIENT_PROPERTY_VALUE;
import static com.ingroupe.efti.edeliveryapconnector.constant.ApConstant.MIME_TYPE;
import static com.ingroupe.efti.edeliveryapconnector.constant.ApConstant.ORIGINAL_SENDER_PROPERTY_KEY;
import static com.ingroupe.efti.edeliveryapconnector.constant.ApConstant.ORIGINAL_SENDER_PROPERTY_VALUE;
import static com.ingroupe.efti.edeliveryapconnector.constant.ApConstant.PARTY_FROM_ROLE;
import static com.ingroupe.efti.edeliveryapconnector.constant.ApConstant.PARTY_TO_ROLE;
import static com.ingroupe.efti.edeliveryapconnector.constant.ApConstant.PARTY_TYPE;
import static com.ingroupe.efti.edeliveryapconnector.constant.ApConstant.PAYLOAD_HREF;
import static com.ingroupe.efti.edeliveryapconnector.constant.ApConstant.SERVICE_TYPE;
import static com.ingroupe.efti.edeliveryapconnector.constant.ApConstant.SERVICE_VALUE;
import static com.ingroupe.efti.edeliveryapconnector.constant.ApConstant.TEXT_PLAIN;
import static org.springframework.util.MimeTypeUtils.TEXT_XML_VALUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestSendingService extends AbstractApService {

    public String sendRequest(final ApRequestDto requestDto, final EDeliveryAction eDeliveryAction) throws SendRequestException {
        final Messaging messaging = createMessaging(requestDto, eDeliveryAction);
        final SubmitRequest submitRequest = createSubmitRequest(requestDto);

        final SubmitResponse submitResponse = sendRequestToAPOrThrow(requestDto, submitRequest, messaging);

        if(submitResponse.getMessageID().isEmpty()) {
            log.error("no messageId for request {}", requestDto.getRequestId());
            throw new SendRequestException("no messageId for request " + requestDto.getRequestId());
        }
        return submitResponse.getMessageID().get(0);
    }

    private SubmitResponse sendRequestToAPOrThrow(final ApRequestDto requestDto, final SubmitRequest submitRequest, final Messaging messaging) throws SendRequestException {
        try {
            return initApWebService(requestDto.getApConfig()).submitMessage(submitRequest, messaging);
        } catch (final SubmitMessageFault | MalformedURLException | InaccessibleWSDLException | ClientTransportException e ) {
            throw new SendRequestException("error while sending request", e);
        }
    }

    private SubmitRequest createSubmitRequest(final ApRequestDto requestDto) throws SendRequestException {
        final SubmitRequest submitRequest = new SubmitRequest();
        final LargePayloadType largePayloadType = new LargePayloadType();

        largePayloadType.setContentType(TEXT_XML_VALUE);
        largePayloadType.setPayloadId(PAYLOAD_HREF);
        final DataSource ds;
        try {
            ds = new ByteArrayDataSource(requestDto.getBody(), TEXT_PLAIN);
        } catch (final IOException e) {
            throw new SendRequestException("error while building request body for request " + requestDto.getRequestId(), e);
        }
        largePayloadType.setValue(new DataHandler(ds));

        submitRequest.getPayload().add(largePayloadType);
        return submitRequest;
    }

    private Messaging createMessaging(final ApRequestDto requestDto, final EDeliveryAction eDeliveryAction) {
        final Messaging messaging = new Messaging();

        final UserMessage userMessage = new UserMessage();
        userMessage.setProcessingType(ProcessingType.PUSH);
        userMessage.setPartyInfo(createPartyInfo(requestDto));
        userMessage.setCollaborationInfo(createCollaborationInfo(eDeliveryAction));
        userMessage.setPayloadInfo(createPayloadInfo());
        userMessage.setMessageProperties(createMessageProperties());

        messaging.setUserMessage(userMessage);
        return messaging;
    }

    private MessageProperties createMessageProperties() {
        final MessageProperties messageProperties = new MessageProperties();
        final Property property = new Property();
        property.setName(ORIGINAL_SENDER_PROPERTY_KEY);
        property.setValue(ORIGINAL_SENDER_PROPERTY_VALUE);
        final Property property2 = new Property();
        property2.setName(FINAL_RECIPIENT_PROPERTY_KEY);
        property2.setValue(FINAL_RECIPIENT_PROPERTY_VALUE);
        messageProperties.getProperty().add(property);
        messageProperties.getProperty().add(property2);
        return messageProperties;
    }

    private PayloadInfo createPayloadInfo() {
        final PayloadInfo payloadInfo = new PayloadInfo();
        final PartInfo partInfo = new PartInfo();
        partInfo.setHref(PAYLOAD_HREF);
        final PartProperties partProperties = new PartProperties();
        final Property property1 = new Property();
        property1.setValue(TEXT_XML_VALUE);
        property1.setName(MIME_TYPE);
        partProperties.getProperty().add(property1);
        partInfo.setPartProperties(partProperties);
        payloadInfo.getPartInfo().add(partInfo);
        return payloadInfo;
    }

    private CollaborationInfo createCollaborationInfo(final EDeliveryAction eDeliveryAction) {
        final CollaborationInfo collaborationInfo = new CollaborationInfo();
        final Service service = new Service();
        service.setType(SERVICE_TYPE);
        service.setValue(SERVICE_VALUE);
        collaborationInfo.setAction(eDeliveryAction.getValue());
        collaborationInfo.setService(service);

        return collaborationInfo;
    }

    private PartyInfo createPartyInfo(final ApRequestDto requestDto) {
        final From from = new From();
        from.setRole(PARTY_FROM_ROLE);
        final PartyId sender = createPartyId(requestDto.getSender());
        from.setPartyId(sender);

        final To to = new To();
        final PartyId receiver = createPartyId(requestDto.getReceiver());
        to.setRole(PARTY_TO_ROLE);
        to.setPartyId(receiver);

        final PartyInfo partyInfo = new PartyInfo();
        partyInfo.setFrom(from);
        partyInfo.setTo(to);
        return partyInfo;
    }

    private PartyId createPartyId(final String value) {
        final PartyId partyId = new PartyId();
        partyId.setType(PARTY_TYPE);
        partyId.setValue(value);
        return partyId;
    }
}
