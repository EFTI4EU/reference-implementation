package eu.efti.edeliveryapconnector.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "body")
public class MessageBodyDto {
    private String requestUuid;
    @XmlElement(name = "eFTIDataUuid")
    private String eFTIDataUuid;
    private String status;
    private String errorDescription;
    @XmlElement(name = "eFTIData")
    private Object eFTIData;
    @XmlElement(name = "eFTIPlatformUrl")
    private String eFTIPlatformUrl;
}
