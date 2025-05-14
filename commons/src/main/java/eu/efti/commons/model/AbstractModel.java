package eu.efti.commons.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.time.LocalDateTime;

@MappedSuperclass
@Data
public abstract class AbstractModel implements Serializable {

    /**
     * date that the data have been created
     */
    @CreatedDate
    @Column(updatable = false, name = "createddate")
    private LocalDateTime createdDate;

    /**
     * date that the data have been modified
     */
    @LastModifiedDate
    @Column(name = "lastmodifieddate")
    private LocalDateTime lastModifiedDate;
}
