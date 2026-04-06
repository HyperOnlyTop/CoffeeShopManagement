package com.example.QuanLyQuanCafe.repository;

import com.example.QuanLyQuanCafe.model.ChatConversation;
import com.example.QuanLyQuanCafe.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationOrderByCreatedAtAsc(ChatConversation conversation);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.readByRecipient = true WHERE m.conversation = :conv AND m.senderType = :senderType AND m.readByRecipient = false")
    int markAsReadBySenderType(@Param("conv") ChatConversation conv, @Param("senderType") ChatMessage.SenderType senderType);
}
