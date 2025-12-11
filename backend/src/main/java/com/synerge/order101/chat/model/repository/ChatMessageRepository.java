package com.synerge.order101.chat.model.repository;


import com.synerge.order101.chat.model.entity.ChatRoom;
import com.synerge.order101.chat.model.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);
}
