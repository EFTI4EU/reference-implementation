package eu.efti.edeliveryapconnector.dto;

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
public class NotesMessageBodyDto {
    private String requestUuid;
    @XmlElement(name = "eFTIPlatformUrl")
    private String eFTIPlatformUrl;
    @XmlElement(name = "eFTIDataUuid")
    private String eFTIDataUuid;
    private String note;
}
