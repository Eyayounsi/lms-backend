package com.elearning.ProjetPfe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);

    long countByConversationIdAndReadFalseAndSenderIdNot(Long conversationId, Long userId);

    List<Message> findByConversationIdAndReadFalseAndSenderIdNot(Long conversationId, Long userId);
}
