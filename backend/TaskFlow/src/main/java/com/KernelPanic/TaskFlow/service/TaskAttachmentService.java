package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.TaskAttachmentResponse;
import com.KernelPanic.TaskFlow.entity.Task;
import com.KernelPanic.TaskFlow.entity.TaskAttachment;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.repository.TaskAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class TaskAttachmentService {
    private static final long MAX_SIZE = 20L * 1024 * 1024;
    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"), Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"), Map.entry("webp", "image/webp"), Map.entry("pdf", "application/pdf"),
            Map.entry("txt", "text/plain"), Map.entry("csv", "text/csv"), Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"));
    private final TaskAttachmentRepository attachments;
    private final TaskService taskService;
    @Value("${taskflow.attachments.directory:./taskflow-data/attachments}") private String directory;

    @Transactional(readOnly = true)
    public List<TaskAttachmentResponse> list(Long taskId, User user) {
        Task task = taskService.findEntity(taskId);
        taskService.ensureCanAccessTask(task, user);
        return attachments.findByTaskIdOrderByCreatedAtDesc(taskId).stream().map(TaskAttachmentResponse::fromEntity).toList();
    }

    @Transactional
    public TaskAttachmentResponse upload(Long taskId, MultipartFile file, User user) {
        Task task = taskService.findEntity(taskId);
        taskService.ensureCanAccessTask(task, user);
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um arquivo não vazio.");
        if (file.getSize() > MAX_SIZE) throw new ResponseStatusException(org.springframework.http.HttpStatusCode.valueOf(413), "O limite por arquivo é 20 MB.");
        String original = file.getOriginalFilename() == null ? "arquivo" : file.getOriginalFilename().replace('\\', '/');
        original = original.substring(original.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\u0000]", "").trim();
        if (original.isBlank() || original.length() > 255) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo inválido.");
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);
        String contentType = TYPES.get(ext);
        if (contentType == null) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de arquivo não permitido.");
        try {
            byte[] content = file.getBytes();
            validateSignature(ext, content);
            String key = UUID.randomUUID().toString();
            Path root = Path.of(directory).toAbsolutePath().normalize();
            Files.createDirectories(root);
            Files.write(root.resolve(key), content);
            try {
                TaskAttachment saved = attachments.saveAndFlush(TaskAttachment.builder().task(task).uploader(user).originalName(original)
                        .storageKey(key).contentType(contentType).sizeBytes((long) content.length).build());
                return TaskAttachmentResponse.fromEntity(saved);
            } catch (RuntimeException e) {
                Files.deleteIfExists(root.resolve(key));
                throw e;
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível armazenar o arquivo.", e);
        }
    }

    @Transactional(readOnly = true)
    public Download download(Long taskId, Long attachmentId, User user) {
        Task task = taskService.findEntity(taskId);
        taskService.ensureCanAccessTask(task, user);
        TaskAttachment attachment = find(taskId, attachmentId);
        try {
            Path path = Path.of(directory).toAbsolutePath().normalize().resolve(attachment.getStorageKey()).normalize();
            if (!path.getParent().equals(Path.of(directory).toAbsolutePath().normalize()) || !Files.isRegularFile(path)) throw new IOException("missing");
            return new Download(new ByteArrayResource(Files.readAllBytes(path)), attachment);
        } catch (IOException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado."); }
    }

    @Transactional
    public void delete(Long taskId, Long attachmentId, User user) {
        Task task = taskService.findEntity(taskId);
        taskService.ensureCanAccessTask(task, user);
        TaskAttachment attachment = find(taskId, attachmentId);
        boolean moderator = user.getRole() == Role.MASTER || user.getRole() == Role.ADMIN
                || (user.getRole() == Role.PO && task.getProject().getOwner().getId().equals(user.getId()));
        if (!moderator && !attachment.getUploader().getId().equals(user.getId())) throw new org.springframework.security.access.AccessDeniedException("Você só pode excluir seus próprios anexos.");
        attachments.delete(attachment);
        try { Files.deleteIfExists(Path.of(directory).toAbsolutePath().normalize().resolve(attachment.getStorageKey())); }
        catch (IOException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Metadados removidos, mas não foi possível limpar o arquivo.", e); }
    }

    private TaskAttachment find(Long taskId, Long id) {
        return attachments.findByIdAndTaskId(id, taskId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anexo não encontrado."));
    }

    private void validateSignature(String ext, byte[] bytes) {
        boolean valid = switch (ext) {
            case "jpg", "jpeg" -> bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255;
            case "png" -> bytes.length >= 8 && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71;
            case "gif" -> bytes.length >= 6 && new String(bytes, 0, 3, java.nio.charset.StandardCharsets.US_ASCII).equals("GIF");
            case "webp" -> bytes.length >= 12 && new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF") && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
            case "pdf" -> bytes.length >= 5 && new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-");
            case "docx", "xlsx", "pptx" -> bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 3 && bytes[3] == 4;
            case "txt", "csv" -> java.util.stream.IntStream.range(0, bytes.length).noneMatch(i -> bytes[i] == 0);
            default -> false;
        };
        if (!valid) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "O conteúdo do arquivo não corresponde ao formato informado.");
    }

    public record Download(Resource resource, TaskAttachment attachment) {}
}