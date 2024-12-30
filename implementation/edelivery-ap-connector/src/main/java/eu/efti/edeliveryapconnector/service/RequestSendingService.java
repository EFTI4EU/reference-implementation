package eu.efti.edeliveryapconnector.service;

import eu.efti.edeliveryapconnector.constant.ApConstant;
import eu.efti.edeliveryapconnector.dto.ApRequestDto;
import eu.efti.edeliveryapconnector.exception.SendRequestException;
import eu.efti.plugin.ws.generated.SubmitMessageFault;
import eu.efti.plugin.ws.generated.body.LargePayloadType;
import eu.efti.plugin.ws.generated.body.SubmitRequest;
import eu.efti.plugin.ws.generated.body.SubmitResponse;
import eu.efti.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.CollaborationInfo;
import eu.efti.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.From;
import eu.efti.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageProperties;
import eu.efti.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import eu.efti.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartInfo;
import eu.efti.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartProperties;
import eu.efti.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyId;
import eu.efti.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyInfo;
import eu.efti.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PayloadInfo;
import eu.efti.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.ProcessingType;
import eu.efti.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Property;
import eu.efti.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Service;
import eu.efti.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.To;
import eu.efti.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;

import static org.springframework.util.MimeTypeUtils.TEXT_XML_VALUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestSendingService extends AbstractApService {

    private static final String ACTION = "eftiGateAction";

    public String sendRequest(final ApRequestDto requestDto) throws SendRequestException {
        final Messaging messaging = createMessaging(requestDto);
        final SubmitRequest submitRequest = createSubmitRequest(requestDto);

        final SubmitResponse submitResponse = sendRequestToAPOrThrow(requestDto, submitRequest, messaging);

        if (submitResponse.getMessageID().isEmpty()) {
            log.error("no messageId for request {}", requestDto.getRequestId());
            throw new SendRequestException("no messageId for request " + requestDto.getRequestId());
        }
        return submitResponse.getMessageID().get(0);
    }

    private SubmitResponse sendRequestToAPOrThrow(final ApRequestDto requestDto, final SubmitRequest submitRequest, final Messaging messaging) throws SendRequestException {
        try {
            return initApWebService(requestDto.getApConfig()).submitMessage(submitRequest, messaging);
        } catch (SubmitMessageFault | MalformedURLException e) {
            throw new SendRequestException("error while sending request", e);
        }
    }

    private SubmitRequest createSubmitRequest(final ApRequestDto requestDto) throws SendRequestException {
        final SubmitRequest submitRequest = new SubmitRequest();
        final LargePayloadType largePayloadType = new LargePayloadType();

        largePayloadType.setContentType(TEXT_XML_VALUE);
        largePayloadType.setPayloadId(ApConstant.PAYLOAD_HREF);

        final DataSource ds = new ByteArrayDataSource(requestDto.getBody().getBytes(StandardCharsets.UTF_8), ApConstant.TEXT_PLAIN);

        largePayloadType.setValue(new DataHandler(ds));

        submitRequest.getPayload().add(largePayloadType);
        return submitRequest;
    }

    private Messaging createMessaging(final ApRequestDto requestDto) {
        final Messaging messaging = new Messaging();

        final UserMessage userMessage = new UserMessage();
        userMessage.setProcessingType(ProcessingType.PUSH);
        userMessage.setPartyInfo(createPartyInfo(requestDto));
        userMessage.setCollaborationInfo(createCollaborationInfo(requestDto.getRequestId()));
        userMessage.setPayloadInfo(createPayloadInfo());
        userMessage.setMessageProperties(createMessageProperties());

        messaging.setUserMessage(userMessage);
        return messaging;
    }

    private MessageProperties createMessageProperties() {
        final MessageProperties messageProperties = new MessageProperties();
        final Property property = new Property();
        property.setName(ApConstant.ORIGINAL_SENDER_PROPERTY_KEY);
        property.setValue(ApConstant.ORIGINAL_SENDER_PROPERTY_VALUE);
        final Property property2 = new Property();
        property2.setName(ApConstant.FINAL_RECIPIENT_PROPERTY_KEY);
        property2.setValue(ApConstant.FINAL_RECIPIENT_PROPERTY_VALUE);
        messageProperties.getProperty().add(property);
        messageProperties.getProperty().add(property2);
        return messageProperties;
    }

    private PayloadInfo createPayloadInfo() {
        final PayloadInfo payloadInfo = new PayloadInfo();
        final PartInfo partInfo = new PartInfo();
        partInfo.setHref(ApConstant.PAYLOAD_HREF);
        final PartProperties partProperties = new PartProperties();
        final Property property1 = new Property();
        property1.setValue(TEXT_XML_VALUE);
        property1.setName(ApConstant.MIME_TYPE);
        partProperties.getProperty().add(property1);
        partInfo.setPartProperties(partProperties);
        payloadInfo.getPartInfo().add(partInfo);
        return payloadInfo;
    }

    private CollaborationInfo createCollaborationInfo(final String conversationId) {
        final CollaborationInfo collaborationInfo = new CollaborationInfo();
        final Service service = new Service();
        service.setType(ApConstant.SERVICE_TYPE);
        service.setValue(ApConstant.SERVICE_VALUE);
        collaborationInfo.setAction(ACTION);
        collaborationInfo.setService(service);
        collaborationInfo.setConversationId(conversationId);

        return collaborationInfo;
    }

    private PartyInfo createPartyInfo(final ApRequestDto requestDto) {
        final From from = new From();
        from.setRole(ApConstant.PARTY_FROM_ROLE);
        final PartyId sender = createPartyId(requestDto.getSender());
        from.setPartyId(sender);

        final To to = new To();
        final PartyId receiver = createPartyId(requestDto.getReceiver());
        to.setRole(ApConstant.PARTY_TO_ROLE);
        to.setPartyId(receiver);

        final PartyInfo partyInfo = new PartyInfo();
        partyInfo.setFrom(from);
        partyInfo.setTo(to);
        return partyInfo;
    }

    private PartyId createPartyId(final String value) {
        final PartyId partyId = new PartyId();
        partyId.setType(ApConstant.PARTY_TYPE);
        partyId.setValue(value);
        return partyId;
    }
}
