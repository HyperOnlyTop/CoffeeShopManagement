package com.example.QuanLyQuanCafe.service;

import com.example.QuanLyQuanCafe.model.*;
import com.example.QuanLyQuanCafe.repository.ChatConversationRepository;
import com.example.QuanLyQuanCafe.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChatService {

    private final ChatConversationRepository conversationRepo;
    private final ChatMessageRepository messageRepo;

    public ChatService(ChatConversationRepository conversationRepo, ChatMessageRepository messageRepo) {
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
    }

    private static final String WELCOME_MESSAGE = "Chào mừng bạn đến với Brew & Co.! Cảm ơn bạn đã liên hệ với chúng tôi. Chúng tôi sẽ phản hồi bạn sớm nhất có thể.";

    @Transactional
    public ChatConversation getOrCreateConversationForGuest(String sessionId, String guestName) {
        Optional<ChatConversation> existing = conversationRepo.findByGuestSessionIdAndStatus(sessionId, ConversationStatus.OPEN);
        if (existing.isPresent()) {
            return existing.get();
        }
        ChatConversation conv = new ChatConversation();
        conv.setGuestSessionId(sessionId);
        conv.setGuestName(guestName);
        conv.setStatus(ConversationStatus.OPEN);
        return conversationRepo.save(conv);
    }

    @Transactional
    public ChatConversation getOrCreateConversationForUser(AppUser user) {
        Optional<ChatConversation> existing = conversationRepo.findByUserAndStatus(user, ConversationStatus.OPEN);
        if (existing.isPresent()) {
            return existing.get();
        }
        ChatConversation conv = new ChatConversation();
        conv.setUser(user);
        conv.setGuestName(user.getFullName());
        conv.setGuestEmail(user.getEmail());
        conv.setGuestPhone(user.getPhone());
        conv.setStatus(ConversationStatus.OPEN);
        return conversationRepo.save(conv);
    }

    @Transactional
    public ChatMessage sendCustomerMessage(ChatConversation conv, String content, AppUser user) {
        boolean isFirstMessage = messageRepo.findByConversationOrderByCreatedAtAsc(conv).isEmpty();

        ChatMessage msg = new ChatMessage();
        msg.setConversation(conv);
        msg.setSenderType(ChatMessage.SenderType.CUSTOMER);
        msg.setSenderUser(user);
        msg.setContent(content);
        messageRepo.save(msg);

        conv.setLastMessageAt(LocalDateTime.now());
        conv.setUnreadByStaff(conv.getUnreadByStaff() + 1);
        conversationRepo.save(conv);

        if (isFirstMessage) {
            addWelcomeMessage(conv);
        }

        return msg;
    }

    private void addWelcomeMessage(ChatConversation conv) {
        ChatMessage welcomeMsg = new ChatMessage();
        welcomeMsg.setConversation(conv);
        welcomeMsg.setSenderType(ChatMessage.SenderType.STAFF);
        welcomeMsg.setSenderUser(null);
        welcomeMsg.setContent(WELCOME_MESSAGE);
        welcomeMsg.setReadByRecipient(false);
        messageRepo.save(welcomeMsg);
        
        conv.setUnreadByCustomer(conv.getUnreadByCustomer() + 1);
        conversationRepo.save(conv);
    }

    @Transactional
    public ChatMessage sendStaffMessage(ChatConversation conv, String content, AppUser staffUser) {
        ChatMessage msg = new ChatMessage();
        msg.setConversation(conv);
        msg.setSenderType(ChatMessage.SenderType.STAFF);
        msg.setSenderUser(staffUser);
        msg.setContent(content);
        messageRepo.save(msg);

        conv.setLastMessageAt(LocalDateTime.now());
        conv.setUnreadByCustomer(conv.getUnreadByCustomer() + 1);
        if (conv.getAssignedStaff() == null) {
            conv.setAssignedStaff(staffUser);
        }
        conversationRepo.save(conv);

        return msg;
    }

    public List<ChatConversation> getOpenConversationsForStaff() {
        return conversationRepo.findOpenConversationsForStaff(ConversationStatus.OPEN);
    }

    public Optional<ChatConversation> findById(Long id) {
        return conversationRepo.findById(id);
    }

    public List<ChatMessage> getMessages(ChatConversation conv) {
        return messageRepo.findByConversationOrderByCreatedAtAsc(conv);
    }

    @Transactional
    public void markReadByStaff(ChatConversation conv) {
        messageRepo.markAsReadBySenderType(conv, ChatMessage.SenderType.CUSTOMER);
        conv.setUnreadByStaff(0);
        conversationRepo.save(conv);
    }

    @Transactional
    public void markReadByCustomer(ChatConversation conv) {
        messageRepo.markAsReadBySenderType(conv, ChatMessage.SenderType.STAFF);
        conv.setUnreadByCustomer(0);
        conversationRepo.save(conv);
    }

    @Transactional
    public void closeConversation(ChatConversation conv) {
        conv.setStatus(ConversationStatus.CLOSED);
        conversationRepo.save(conv);
    }

    public int getTotalUnreadForStaff() {
        return conversationRepo.countTotalUnreadByStaff(ConversationStatus.OPEN);
    }

    public Optional<ChatConversation> findOpenByGuestSession(String sessionId) {
        return conversationRepo.findByGuestSessionIdAndStatus(sessionId, ConversationStatus.OPEN);
    }

    public Optional<ChatConversation> findOpenByUser(AppUser user) {
        return conversationRepo.findByUserAndStatus(user, ConversationStatus.OPEN);
    }
}
