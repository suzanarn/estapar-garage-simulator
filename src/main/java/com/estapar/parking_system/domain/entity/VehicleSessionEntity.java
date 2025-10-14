package com.estapar.parking_system.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "vehicle_session")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VehicleSessionEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @ManyToOne(optional = true)
    @JoinColumn(name = "sector_id", nullable = true)
    private SectorEntity sector;

    @ManyToOne
    @JoinColumn(name = "spot_id")
    private SpotEntity spot;

    @Column(name = "entry_time", nullable = false)
    private Instant entryTime;

    @Column(name = "exit_time")
    private Instant exitTime;

    @Column(name = "base_price", nullable = true, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "price_factor", nullable = false, precision = 5, scale = 2)
    private BigDecimal priceFactor;

    @Column(name = "charged_amount", precision = 10, scale = 2)
    private BigDecimal chargedAmount;
}
