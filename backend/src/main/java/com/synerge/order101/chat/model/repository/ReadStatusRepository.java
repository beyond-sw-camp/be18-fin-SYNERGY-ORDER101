package com.synerge.order101.chat.model.repository;

import com.synerge.order101.chat.model.entity.ChatRoom;
import com.synerge.order101.chat.model.entity.MessageReadStatus;
import com.synerge.order101.user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadStatusRepository extends JpaRepository<MessageReadStatus, Integer> {

    @Modifying
    @Query(value = """
        UPDATE message_read_status
        SET is_read = 1
        WHERE chat_room_id = :roomId
          AND user_id = :userId
          AND is_read = 0
    """, nativeQuery = true)
    int markAsReadIgnore(@Param("roomId") long roomId, @Param("userId") long userId);
}
