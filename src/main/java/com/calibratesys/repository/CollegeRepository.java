package com.calibratesys.repository;

import com.calibratesys.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CollegeRepository extends JpaRepository<College, Long> {

    Optional<College> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT l.college.id) FROM Laboratory l WHERE l.id IN :laboratoryIds")
    long countDistinctCollegesByLaboratoryIdIn(@org.springframework.data.repository.query.Param("laboratoryIds") java.util.Collection<Long> laboratoryIds);
}
