package eu.efti.commons.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@MappedSuperclass
@Data
public abstract class AbstractModel implements Serializable {

    /**
     * date that the data have been created
     */
    @CreationTimestamp
    @Column(updatable = false, name = "createddate")
    private OffsetDateTime createdDate;

    /**
     * date that the data have been modified
     */
    @Column(name = "lastmodifieddate")
    private OffsetDateTime lastModifiedDate;

    @PrePersist
    public void onCreate() {
        this.setCreatedDate(OffsetDateTime.now(ZoneId.of("UTC")));
        this.setLastModifiedDate(this.getCreatedDate());
    }

    @PreUpdate
    public void onUpdate() {
        this.setLastModifiedDate(OffsetDateTime.now(ZoneId.of("UTC")));
    }
}
