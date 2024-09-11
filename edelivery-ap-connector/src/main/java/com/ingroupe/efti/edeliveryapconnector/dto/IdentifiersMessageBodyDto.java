package com.ingroupe.efti.edeliveryapconnector.dto;

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
@XmlRootElement(name = "body")
public class IdentifiersMessageBodyDto {
    private String requestUuid;
    private String transportMode;
    @XmlElement(name = "vehicleID")
    private String vehicleID;
    private String vehicleCountry;
    @XmlElement(name = "isDangerousGoods")
    private Boolean isDangerousGoods;
    @XmlElement(name = "eFTIGateIndicator")
    private List<String> eFTIGateIndicator;
}
