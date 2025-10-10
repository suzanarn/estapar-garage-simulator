package com.estapar.parking_system.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "spot")
public class SpotEntity {
    @Id
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sector_id")
    private SectorEntity sector;

    @Column(name = "lat")
    private BigDecimal lat;

    @Column(name = "lng")
    private BigDecimal lng;

    @Column(name = "occupied_by_session_id")
    private Long occupiedBySessionId;
}
