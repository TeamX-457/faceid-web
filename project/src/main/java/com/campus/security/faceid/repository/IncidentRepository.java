package com.campus.security.faceid.repository;

import com.campus.security.faceid.model.Incident;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByUploadedByUserIdOrderByTimestampDesc(Long uploadedByUserId);
}