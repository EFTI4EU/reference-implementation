package eu.efti.plugin.ws.client;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import lombok.extern.slf4j.Slf4j;

import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MessageLoggingHandler implements SOAPHandler<SOAPMessageContext> {

    @Override
    public Set<QName> getHeaders() {
        return new HashSet<>();
    }

    @Override
    public boolean handleMessage(final SOAPMessageContext context) {
        boolean isRequest = (boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (isRequest) {
            log.info("======== Logging Request ========");
            logSOAPMessage(context.getMessage(), false);
        } else {
            log.debug("======== Logging Response ========");
            logSOAPMessage(context.getMessage(), true);
        }

        //continue with message processing
        return true;
    }

    @Override
    public boolean handleFault(final SOAPMessageContext context) {
        log.info("======== Logging SOAPFault ========");
        logSOAPMessage(context.getMessage(), false);
        return true;
    }

    @Override
    public void close(final MessageContext messageContext) {
        //do nothing
    }

    private void logSOAPMessage(final SOAPMessage message, final boolean debug) {
        String output = null;
        try {
            output = convertToString(message);
        } catch (IOException | SOAPException e) {
            log.error("", e);
        }
        if (output != null) {
            if (debug) {
                log.debug(output);
            } else {
                log.info(output);
            }
        }
    }

    private String convertToString(final SOAPMessage message) throws IOException, SOAPException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        message.writeTo(stream);
        return stream.toString(StandardCharsets.UTF_8);
    }
}
