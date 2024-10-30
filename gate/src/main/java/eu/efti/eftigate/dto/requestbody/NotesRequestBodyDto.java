package eu.efti.eftigate.dto.requestbody;

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
@XmlRootElement(name = "body")
@XmlAccessorType(XmlAccessType.FIELD)
public class NotesRequestBodyDto {
    private String requestId;
    @XmlElement(name = "eFTIPlatformUrl")
    private String eFTIPlatformUrl;
    @XmlElement(name = "eFTIGateUrl")
    private String eFTIGateUrl;
    @XmlElement(name = "eFTIDataUuid")
    private String eFTIDataUuid;
    private String note;
}
