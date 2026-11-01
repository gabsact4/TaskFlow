package com.KernelPanic.TaskFlow.repository;
import com.KernelPanic.TaskFlow.entity.ProductivityGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ProductivityGoalRepository extends JpaRepository<ProductivityGoal, Long> {
    List<ProductivityGoal> findByUserIdOrderByPeriodTypeAscMetricAsc(Long userId);
    Optional<ProductivityGoal> findByIdAndUserId(Long id, Long userId);
    Optional<ProductivityGoal> findByUserIdAndMetricAndPeriodType(Long userId, ProductivityGoal.Metric metric, ProductivityGoal.PeriodType periodType);
}