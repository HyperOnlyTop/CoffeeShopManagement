package com.example.QuanLyQuanCafe.repository;

import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.model.ChatConversation;
import com.example.QuanLyQuanCafe.model.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findByGuestSessionIdAndStatus(String guestSessionId, ConversationStatus status);

    Optional<ChatConversation> findByUserAndStatus(AppUser user, ConversationStatus status);

    List<ChatConversation> findByStatusOrderByLastMessageAtDesc(ConversationStatus status);

    @Query("SELECT c FROM ChatConversation c WHERE c.status = :status ORDER BY c.unreadByStaff DESC, c.lastMessageAt DESC")
    List<ChatConversation> findOpenConversationsForStaff(@Param("status") ConversationStatus status);

    @Query("SELECT COALESCE(SUM(c.unreadByStaff), 0) FROM ChatConversation c WHERE c.status = :status")
    int countTotalUnreadByStaff(@Param("status") ConversationStatus status);
}
