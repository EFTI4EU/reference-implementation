package eu.efti.plugin.ws.client;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageLoggingHandlerTest {

    private MessageLoggingHandler handler;
    @Mock
    private SOAPMessageContext soapMessageContext;
    @Mock
    private SOAPMessage soapMessage;
    private LogCaptor logCaptor;

    @BeforeEach
    void setup() {
        handler = new MessageLoggingHandler();
        logCaptor = LogCaptor.forClass(MessageLoggingHandler.class);
        logCaptor.setLogLevelToTrace();
        logCaptor.clearLogs();
    }

    @Test
    void testLogRequestMessage() throws Exception {
        when(soapMessageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)).thenReturn(true);
        when(soapMessageContext.getMessage()).thenReturn(soapMessage);

        doAnswer(invocation -> {
            ByteArrayOutputStream os = invocation.getArgument(0);
            os.write("dummy-request".getBytes());
            return null;
        }).when(soapMessage).writeTo(any());

        handler.handleMessage(soapMessageContext);

        assertTrue(logCaptor.getInfoLogs().stream().anyMatch(log -> log.contains("Logging Request")));
        assertTrue(logCaptor.getInfoLogs().stream().anyMatch(log -> log.contains("dummy-request")));
    }

    @Test
    void testLogResponseMessage() throws Exception {
        when(soapMessageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)).thenReturn(false);
        when(soapMessageContext.getMessage()).thenReturn(soapMessage);

        doAnswer(invocation -> {
            ByteArrayOutputStream os = invocation.getArgument(0);
            os.write("dummy-response".getBytes());
            return null;
        }).when(soapMessage).writeTo(any());

        handler.handleMessage(soapMessageContext);

        assertTrue(logCaptor.getDebugLogs().stream().anyMatch(log -> log.contains("Logging Response")));
        assertTrue(logCaptor.getDebugLogs().stream().anyMatch(log -> log.contains("dummy-response")));
    }

    @Test
    void testLogFaultMessage() throws Exception {
        when(soapMessageContext.getMessage()).thenReturn(soapMessage);

        doAnswer(invocation -> {
            ByteArrayOutputStream os = invocation.getArgument(0);
            os.write("soap-fault".getBytes());
            return null;
        }).when(soapMessage).writeTo(any());

        handler.handleFault(soapMessageContext);

        assertTrue(logCaptor.getInfoLogs().stream().anyMatch(log -> log.contains("Logging SOAPFault")));
        assertTrue(logCaptor.getInfoLogs().stream().anyMatch(log -> log.contains("soap-fault")));
    }

    @Test
    void testHandleMessageLogsSOAPException() throws Exception {
        when(soapMessageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)).thenReturn(true);
        when(soapMessageContext.getMessage()).thenReturn(soapMessage);

        doThrow(new SOAPException("Simulated SOAP failure")).when(soapMessage).writeTo(any());

        boolean result = handler.handleMessage(soapMessageContext);

        assertTrue(result);
        assertTrue(logCaptor.getErrorLogs().stream().anyMatch(String::isBlank));
    }
}
