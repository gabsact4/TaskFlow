package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.NotificationResponse;
import com.KernelPanic.TaskFlow.entity.Notification;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.enums.TaskStatus;
import com.KernelPanic.TaskFlow.repository.NotificationRepository;
import com.KernelPanic.TaskFlow.repository.TaskRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(User user) {
        return notificationRepository.findTop100ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return notificationRepository.countByUserIdAndReadAtIsNull(user.getId());
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada."));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(User user) {
        notificationRepository.markAllRead(user.getId(), Instant.now());
    }

    @Transactional
    public void notifyProjectOwnerOfTaskStatus(Task task, TaskStatus status) {
        if (status != TaskStatus.REVIEW && status != TaskStatus.DONE) return;
        User owner = task.getProject().getOwner();
        if (owner.getRole() != Role.PO) return;
        String label = status == TaskStatus.REVIEW ? "está em teste" : "foi concluída";
        String title = status == TaskStatus.REVIEW ? "Tarefa pronta para teste" : "Tarefa concluída";
        String eventKey = "task:" + task.getId() + ":po:" + owner.getId() + ":status:" + status + ":version:" + task.getVersion();
        createOnce(owner, task, "TASK_STATUS", title,
                "A tarefa “" + task.getTitle() + "” " + label + " no projeto “" + task.getProject().getName() + "”.",
                eventKey);
    }

    /** Creates persistent, deduplicated alerts using each recipient's reminder preference. */
    @Scheduled(fixedDelayString = "${taskflow.notifications.scan-interval-ms:60000}")
    @Transactional
    public void generateDueDateNotifications() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<User> supervisors = userRepository.findByRoleIn(List.of(Role.MASTER, Role.ADMIN));

        for (Task task : taskRepository.findOpenTopLevelTasksWithDueDate(TaskStatus.DONE)) {
            long daysUntilDue = task.getDueDate().toEpochDay() - today.toEpochDay();
            Map<Long, User> recipients = new LinkedHashMap<>();
            addRecipient(recipients, task.getAssignee());
            addRecipient(recipients, task.getCreator());
            addRecipient(recipients, task.getProject().getOwner());
            supervisors.forEach(user -> addRecipient(recipients, user));

            for (User recipient : recipients.values()) {
                if (daysUntilDue < 0) {
                    createOnce(recipient, task, "CRITICAL", "Prazo vencido",
                            task.getTitle() + " venceu há " + Math.abs(daysUntilDue) + " dia(s).",
                            "task:" + task.getId() + ":user:" + recipient.getId() + ":overdue");
                } else if (daysUntilDue <= 1) {
                    String timing = daysUntilDue == 0 ? "vence hoje" : "vence amanhã";
                    createOnce(recipient, task, "CRITICAL", "Prazo crítico: " + timing,
                            task.getTitle() + " " + timing + ".",
                            "task:" + task.getId() + ":user:" + recipient.getId() + ":critical:" + task.getDueDate());
                } else if (daysUntilDue <= recipient.getReminderDays()) {
                    createOnce(recipient, task, "REMINDER", "Lembrete de vencimento",
                            task.getTitle() + " vence em " + daysUntilDue + " dia(s).",
                            "task:" + task.getId() + ":user:" + recipient.getId() + ":reminder:" + task.getDueDate());
                }
            }
        }
    }

    private void createOnce(User user, Task task, String type, String title, String message, String eventKey) {
        notificationRepository.insertIfAbsent(user.getId(), task.getId(), type, title, message, eventKey, Instant.now());
    }

    private void addRecipient(Map<Long, User> recipients, User user) {
        if (user != null) recipients.putIfAbsent(user.getId(), user);
    }
}
