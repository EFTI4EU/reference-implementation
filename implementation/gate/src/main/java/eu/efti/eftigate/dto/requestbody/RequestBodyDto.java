package eu.efti.eftigate.dto.requestbody;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "body")
public class RequestBodyDto {
    @XmlElement(name = "eFTIData")
    private String eFTIData;
    @XmlElement(name = "eFTIPlatformUrl")
    private String eFTIPlatformUrl;
    private String requestUuid;
    @XmlElement(name = "eFTIDataUuid")
    private String eFTIDataUuid;
    private List<String> subsetEU;
    private List<String> subsetMS;
    private AuthorityBodyDto authority;
}
