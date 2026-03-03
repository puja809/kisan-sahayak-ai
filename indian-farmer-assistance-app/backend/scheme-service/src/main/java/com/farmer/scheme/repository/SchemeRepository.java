package com.farmer.scheme.repository;

import com.farmer.scheme.entity.Scheme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, Long> {
    Page<Scheme> findByCommodityNameContainingIgnoreCase(String commodity, Pageable pageable);
    Page<Scheme> findByCenterStateNameContainingIgnoreCase(String state, Pageable pageable);
    Page<Scheme> findByOfficeAddressContainingIgnoreCase(String center, Pageable pageable);
    Page<Scheme> findByCommodityNameContainingIgnoreCaseAndCenterStateNameContainingIgnoreCase(
            String commodity, String state, Pageable pageable);
}
