package com.vierec.modules.supportpoint.repository;

import com.vierec.modules.supportpoint.entity.SupportPoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SupportPointRepository extends JpaRepository<SupportPoint, Long> {

    /** Live support points inside a latitude / longitude box; the service then keeps those within the radius. */
    @Query("SELECT p FROM SupportPoint p WHERE p.latitude BETWEEN :minLat AND :maxLat "
            + "AND p.longitude BETWEEN :minLng AND :maxLng")
    List<SupportPoint> findInBox(@Param("minLat") BigDecimal minLat, @Param("maxLat") BigDecimal maxLat,
                                 @Param("minLng") BigDecimal minLng, @Param("maxLng") BigDecimal maxLng);

    /** Live support points whose name, province, district, address or phone contains the keyword (any case). */
    @Query("SELECT p FROM SupportPoint p WHERE :keyword IS NULL "
            + "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(p.province) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(p.district) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(p.address) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR p.phoneNumber LIKE CONCAT('%', :keyword, '%')")
    Page<SupportPoint> search(@Param("keyword") String keyword, Pageable pageable);
}
