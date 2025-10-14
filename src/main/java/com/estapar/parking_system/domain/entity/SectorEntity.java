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
@Table(name = "sector")
public class SectorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @Column(name= "base_price", nullable = true)
    private BigDecimal basePrice;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "open_hour", nullable = false)
    private String openHour;

    @Column(name = "close_hour", nullable = false)
    private String closeHour;

    @Column(name = "duration_limit_minutes", nullable = false)
    private Integer durationLimitMinutes;

}
