package com.estapar.parking_system.domain.repository;

import com.estapar.parking_system.domain.entity.SpotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SpotRepository extends JpaRepository<SpotEntity, Long> {

    List<SpotEntity> findAllByLatAndLngOrderByIdAsc(BigDecimal lat, BigDecimal lng);

    @Query(value = """
        SELECT *
        FROM spot
        WHERE sector_id = :sectorId
        ORDER BY ABS(lat - :lat) + ABS(lng - :lng) ASC
        LIMIT 1
        """, nativeQuery = true)
    Optional<SpotEntity> findNearestInSector(@Param("sectorId") Long sectorId,
                                             @Param("lat") BigDecimal lat,
                                             @Param("lng") BigDecimal lng);

    @Query(value = """
    SELECT *
    FROM spot
    ORDER BY ABS(lat - :lat) + ABS(lng - :lng) ASC
    LIMIT 1
    """, nativeQuery = true)
    Optional<SpotEntity> findNearestGlobal(@Param("lat") BigDecimal lat,
                                           @Param("lng") BigDecimal lng);


    @Query("""
       select count(s) from SpotEntity s
       where s.sector.id = :sectorId and s.occupiedBySessionId is not null
       """)
    long countOccupiedInSector(@Param("sectorId") Long sectorId);

    @Query("""
       select count(s) from SpotEntity s
       where s.occupiedBySessionId is not null
       """)
    long countOccupiedGlobal();

    @Query("""
       select s from SpotEntity s
       where s.sector.id = :sectorId and s.occupiedBySessionId is null
       order by s.id asc
       """)
    Optional<SpotEntity> findFirstFreeInSector(@Param("sectorId") Long sectorId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
       update SpotEntity s
       set s.occupiedBySessionId = :sessionId
       where s.id = :spotId and s.occupiedBySessionId is null
       """)
    int tryOccupy(@Param("spotId") Long spotId, @Param("sessionId") Long sessionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
       update SpotEntity s
       set s.occupiedBySessionId = :newSessionId
       where s.id = :spotId and s.occupiedBySessionId = :expectedSessionId
       """)
    int trySwapOccupant(@Param("spotId") Long spotId,
                        @Param("expectedSessionId") Long expectedSessionId,
                        @Param("newSessionId") Long newSessionId);
    Optional<SpotEntity> findFirstBySector_IdAndOccupiedBySessionIdIsNullOrderByIdAsc(Long sectorId);

}
