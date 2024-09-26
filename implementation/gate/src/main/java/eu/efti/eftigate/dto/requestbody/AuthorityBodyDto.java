package eu.efti.eftigate.dto.requestbody;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthorityBodyDto {
    private String country;
    private String legalContact;
    private String workingContact;
    private boolean isEmergencyService;
    private String authorityName;
    private String nationalUniqueIdentifier;
}
