package com.nologo2.math2.rest.cache;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculationCacheRepository extends JpaRepository<CalculationCacheEntry, String> {
    List<CalculationCacheEntry> findAllByOrderByLastAccessedAtAscCreatedAtAsc(Pageable pageable);
}
