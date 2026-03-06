package com.elearning.ProjetPfe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE c.participant1.id = :userId OR c.participant2.id = :userId ORDER BY c.lastMessageAt DESC")
    List<Conversation> findByParticipant(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.participant1.id = :user1 AND c.participant2.id = :user2) OR " +
           "(c.participant1.id = :user2 AND c.participant2.id = :user1)")
    java.util.Optional<Conversation> findByParticipants(@Param("user1") Long user1, @Param("user2") Long user2);
}
