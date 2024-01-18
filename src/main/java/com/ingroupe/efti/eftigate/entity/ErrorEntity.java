package com.ingroupe.efti.eftigate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "error", catalog = "efti")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "errorcode")
    private String errorCode;
    
    @Column(name = "errordescription")
    private String errorDescription;
}
