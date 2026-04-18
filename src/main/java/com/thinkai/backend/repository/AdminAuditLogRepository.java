package com.thinkai.backend.repository;

import com.thinkai.backend.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    @Query("""
            SELECT a FROM AdminAuditLog a
            WHERE (:actor IS NULL OR LOWER(a.actor) LIKE LOWER(CONCAT('%', :actor, '%')))
              AND (:action IS NULL OR a.action = :action)
              AND (:resourceType IS NULL OR a.resourceType = :resourceType)
            """)
    Page<AdminAuditLog> search(
            @Param("actor") String actor,
            @Param("action") String action,
            @Param("resourceType") String resourceType,
            Pageable pageable);
}
