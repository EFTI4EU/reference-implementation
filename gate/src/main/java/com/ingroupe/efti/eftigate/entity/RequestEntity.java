package com.ingroupe.efti.eftigate.entity;

import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.model.AbstractModel;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "request_type")
@Table(name = "request", catalog = "efti")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@Convert(attributeName = "entityAttrName", converter = JsonBinaryType.class)
public class RequestEntity extends AbstractModel implements Serializable {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private long id;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private RequestStatusEnum status;
    
    @Column(name = "edeliverymessageid")
    private String edeliveryMessageId;
    
    @Column(name = "retry")
    private Integer retry;

    @Column(name = "nextretrydate")
    private LocalDateTime nextRetryDate;
    
    @Column(name = "gateurldest")
    private String gateUrlDest;

    @ManyToOne
    @JoinColumn(name = "control")
    ControlEntity control;

    @OneToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "error", referencedColumnName = "id")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    ErrorEntity error;

    @Getter
    @Column(name="request_type", insertable = false, updatable = false)
    protected String requestType;
}
