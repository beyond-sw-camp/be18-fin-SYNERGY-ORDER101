package com.synerge.order101.chat.model.repository;


import com.synerge.order101.chat.model.entity.ChatRoom;
import com.synerge.order101.chat.model.entity.JoinedChatRoom;
import com.synerge.order101.user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<JoinedChatRoom, Long> {
    List<JoinedChatRoom> findByChatRoom(ChatRoom chatRoom);

    @Query("SELECT cp1.chatRoom FROM JoinedChatRoom cp1 JOIN JoinedChatRoom cp2 ON cp1.chatRoom.chatRoomId = cp2.chatRoom.chatRoomId where cp1.user.userId = :myId and cp2.user.userId = :otherUserId")
    Optional<ChatRoom> findExistingPrivateRoom(@Param("myId") long myId, @Param("otherUserId") long otherUserId);

    boolean existsByChatRoom_ChatRoomIdAndUser_UserId(Long chatRoomId, Long userId);

}
