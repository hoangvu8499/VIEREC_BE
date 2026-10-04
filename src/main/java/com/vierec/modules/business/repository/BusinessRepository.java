package com.vierec.modules.business.repository;

import com.vierec.modules.business.entity.Business;
import com.vierec.modules.business.entity.BusinessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessRepository extends JpaRepository<Business, Long> {

    boolean existsByTaxCode(String taxCode);

    boolean existsByTaxCodeAndIdNot(String taxCode, Long id);

    /** A null keyword or status matches everything. The keyword is looked up in the name and the tax code. */
    @Query("SELECT b FROM Business b WHERE (:status IS NULL OR b.status = :status) AND (:keyword IS NULL "
            + "OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR b.taxCode LIKE CONCAT('%', :keyword, '%'))")
    Page<Business> search(@Param("keyword") String keyword,
                          @Param("status") BusinessStatus status,
                          Pageable pageable);
}
