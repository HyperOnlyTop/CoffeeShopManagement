package com.example.QuanLyQuanCafe.controller;

import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.model.ChatConversation;
import com.example.QuanLyQuanCafe.model.ChatMessage;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/staff/chat")
@PreAuthorize("hasAnyRole('ADMIN','CASHIER','SERVER')")
public class StaffChatController {

    private final ChatService chatService;
    private final AppUserRepository appUserRepository;

    public StaffChatController(ChatService chatService, AppUserRepository appUserRepository) {
        this.chatService = chatService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations() {
        List<ChatConversation> convs = chatService.getOpenConversationsForStaff();
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM");

        for (ChatConversation c : convs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("displayName", c.getDisplayName());
            item.put("lastMessageAt", c.getLastMessageAt() != null ? c.getLastMessageAt().format(fmt) : "");
            item.put("unread", c.getUnreadByStaff());
            item.put("status", c.getStatus().name());
            if (c.getAssignedStaff() != null) {
                item.put("assignedStaff", c.getAssignedStaff().getFullName());
            }
            result.add(item);
        }

        return ResponseEntity.ok(Map.of(
                "conversations", result,
                "totalUnread", chatService.getTotalUnreadForStaff()
        ));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long id) {
        Optional<ChatConversation> optConv = chatService.findById(id);
        if (optConv.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ChatConversation conv = optConv.get();
        chatService.markReadByStaff(conv);

        List<ChatMessage> messages = chatService.getMessages(conv);
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM");

        for (ChatMessage m : messages) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("content", m.getContent());
            item.put("senderType", m.getSenderType().name());
            item.put("createdAt", m.getCreatedAt().format(fmt));
            if (m.getSenderUser() != null) {
                item.put("senderName", m.getSenderUser().getFullName());
            }
            result.add(item);
        }

        return ResponseEntity.ok(Map.of(
                "messages", result,
                "conversationId", conv.getId(),
                "customerName", conv.getDisplayName()
        ));
    }

    @PostMapping("/conversations/{id}/send")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        String content = body.get("message");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tin nhắn không được để trống"));
        }

        Optional<ChatConversation> optConv = chatService.findById(id);
        if (optConv.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AppUser staffUser = appUserRepository.findByUsername(auth.getName());
        if (staffUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Không xác định được nhân viên"));
        }

        ChatConversation conv = optConv.get();
        ChatMessage msg = chatService.sendStaffMessage(conv, content.trim(), staffUser);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "messageId", msg.getId()
        ));
    }

    @PostMapping("/conversations/{id}/close")
    public ResponseEntity<?> closeConversation(@PathVariable Long id) {
        Optional<ChatConversation> optConv = chatService.findById(id);
        if (optConv.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        chatService.closeConversation(optConv.get());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        return ResponseEntity.ok(Map.of("totalUnread", chatService.getTotalUnreadForStaff()));
    }
}
