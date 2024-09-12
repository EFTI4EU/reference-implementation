package eu.efti.eftigate.entity;

import eu.efti.commons.dto.SearchParameter;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.model.AbstractModel;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "control", catalog = "efti")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Convert(attributeName = "entityAttrName", converter = JsonBinaryType.class)
public class ControlEntity extends AbstractModel implements Serializable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "eftidatauuid")
    private String eftiDataUuid;
    
    @Column(name = "requestuuid")
    private String requestUuid;

    @Column(name = "requesttype")
    @Enumerated(EnumType.STRING)
    private RequestTypeEnum requestType;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private StatusEnum status;

    @Column(name = "eftiplatformurl")
    private String eftiPlatformUrl;

    @Column(name = "eftigateurl")
    private String eftiGateUrl;

    @Column(name = "subseteurequested")
    private String subsetEuRequested;

    @Column(name = "subsetmsrequested")
    private String subsetMsRequested;

    @Column(name = "eftidata")
    private byte[] eftiData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transportidentifiers")
    private SearchParameter transportIdentifiers;

    @Column(name = "fromgateurl")
    private String fromGateUrl;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "control", fetch = FetchType.EAGER)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<RequestEntity> requests;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "authority", referencedColumnName = "id")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private AuthorityEntity authority;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "error", referencedColumnName = "id")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private ErrorEntity error;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "consignment")
    private IdentifiersResults identifiersResults;

    public boolean isExternalAsk() {
        return this.getRequestType() != null && this.getRequestType().isExternalAsk();
    }
}
