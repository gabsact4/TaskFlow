package com.KernelPanic.TaskFlow.repository;
import com.KernelPanic.TaskFlow.entity.ChangeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface ChangeHistoryRepository extends JpaRepository<ChangeHistory, Long> {
    Page<ChangeHistory> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);
    Page<ChangeHistory> findByProjectOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);
    Page<ChangeHistory> findByProjectIdInOrderByCreatedAtDesc(List<Long> projectIds, Pageable pageable);
    Page<ChangeHistory> findByProjectIdInOrProjectOwnerIdOrderByCreatedAtDesc(List<Long> projectIds, Long ownerId, Pageable pageable);
    List<ChangeHistory> findByProjectIdInOrderByCreatedAtDesc(List<Long> projectIds);
    boolean existsByProjectIdAndProjectOwnerId(Long projectId, Long ownerId);
    boolean existsByProjectId(Long projectId);

        @Query("select h from ChangeHistory h where (h.projectId in :projectIds and (h.projectId in :fullProjectIds or h.taskId is null or h.taskId in :taskIds)) or (:includeDeletedOwned = true and h.projectOwnerId = :ownerId and h.projectId not in :projectIds)")
        Page<ChangeHistory> findAccessible(@Param("projectIds") List<Long> projectIds, @Param("fullProjectIds") List<Long> fullProjectIds,
            @Param("taskIds") List<Long> taskIds, @Param("includeDeletedOwned") boolean includeDeletedOwned,
            @Param("ownerId") Long ownerId, Pageable pageable);

        @Query("select h from ChangeHistory h where (h.projectId in :projectIds and (h.projectId in :fullProjectIds or h.taskId is null or h.taskId in :taskIds)) or (:includeDeletedOwned = true and h.projectOwnerId = :ownerId and h.projectId not in :projectIds) order by h.createdAt desc")
        List<ChangeHistory> findAccessibleForExport(@Param("projectIds") List<Long> projectIds, @Param("fullProjectIds") List<Long> fullProjectIds,
            @Param("taskIds") List<Long> taskIds, @Param("includeDeletedOwned") boolean includeDeletedOwned, @Param("ownerId") Long ownerId);
}