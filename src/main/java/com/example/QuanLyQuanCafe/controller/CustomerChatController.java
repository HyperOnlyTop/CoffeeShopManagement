package com.example.QuanLyQuanCafe.controller;

import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.model.ChatConversation;
import com.example.QuanLyQuanCafe.model.ChatMessage;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/chat/support")
public class CustomerChatController {

    private final ChatService chatService;
    private final AppUserRepository appUserRepository;

    public CustomerChatController(ChatService chatService, AppUserRepository appUserRepository) {
        this.chatService = chatService;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @RequestBody Map<String, String> body,
            Authentication auth,
            HttpSession session) {

        String content = body.get("message");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tin nhắn không được để trống"));
        }

        AppUser user = null;
        ChatConversation conv;

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            user = appUserRepository.findByUsername(auth.getName());
        }

        if (user != null) {
            conv = chatService.getOrCreateConversationForUser(user);
        } else {
            String sessionId = session.getId();
            String guestName = body.get("guestName");
            conv = chatService.getOrCreateConversationForGuest(sessionId, guestName);
        }

        ChatMessage msg = chatService.sendCustomerMessage(conv, content.trim(), user);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "messageId", msg.getId(),
                "conversationId", conv.getId()
        ));
    }

    @GetMapping("/messages")
    public ResponseEntity<?> getMessages(Authentication auth, HttpSession session) {
        AppUser user = null;
        ChatConversation conv = null;

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            user = appUserRepository.findByUsername(auth.getName());
        }

        if (user != null) {
            conv = chatService.findOpenByUser(user).orElse(null);
        } else {
            conv = chatService.findOpenByGuestSession(session.getId()).orElse(null);
        }

        if (conv == null) {
            return ResponseEntity.ok(Map.of("messages", Collections.emptyList(), "conversationId", 0));
        }

        chatService.markReadByCustomer(conv);

        List<ChatMessage> messages = chatService.getMessages(conv);
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM");

        for (ChatMessage m : messages) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("content", m.getContent());
            item.put("senderType", m.getSenderType().name());
            item.put("createdAt", m.getCreatedAt().format(fmt));
            if (m.getSenderUser() != null && m.getSenderType() == ChatMessage.SenderType.STAFF) {
                item.put("staffName", m.getSenderUser().getFullName());
            }
            result.add(item);
        }

        return ResponseEntity.ok(Map.of(
                "messages", result,
                "conversationId", conv.getId(),
                "unread", conv.getUnreadByCustomer()
        ));
    }

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadCount(Authentication auth, HttpSession session) {
        AppUser user = null;
        ChatConversation conv = null;

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            user = appUserRepository.findByUsername(auth.getName());
        }

        if (user != null) {
            conv = chatService.findOpenByUser(user).orElse(null);
        } else {
            conv = chatService.findOpenByGuestSession(session.getId()).orElse(null);
        }

        int unread = conv != null ? conv.getUnreadByCustomer() : 0;
        return ResponseEntity.ok(Map.of("unread", unread));
    }
}
