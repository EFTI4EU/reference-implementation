package eu.efti.edeliveryapconnector.service;

import eu.efti.edeliveryapconnector.dto.ApConfigDto;
import eu.efti.plugin.ws.generated.MarkMessageAsDownloadedFault;
import eu.efti.plugin.ws.generated.body.MarkMessageAsDownloadedRequest;
import eu.efti.plugin.ws.generated.body.MarkMessageAsDownloadedResponse;
import eu.efti.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import jakarta.xml.ws.Holder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestUpdaterService extends AbstractApService {

    public void setMarkedAsDownload(final ApConfigDto apConfigDto, final String messageId) throws MalformedURLException {
        final MarkMessageAsDownloadedRequest markMessageAsDownloadedRequest = new MarkMessageAsDownloadedRequest();
        markMessageAsDownloadedRequest.setMessageID(messageId);
        final Holder<MarkMessageAsDownloadedResponse> holderResponse = new Holder<>(new MarkMessageAsDownloadedResponse());
        final Holder<Messaging> holderMessaging = new Holder<>(new Messaging());

        try {
            initApWebService(apConfigDto).markMessageAsDownloaded(markMessageAsDownloadedRequest, holderResponse, holderMessaging);
        } catch (final MarkMessageAsDownloadedFault | MalformedURLException e) {
            throw new MalformedURLException("Error while try to set message marked as downloaded");
        }
    }
}
