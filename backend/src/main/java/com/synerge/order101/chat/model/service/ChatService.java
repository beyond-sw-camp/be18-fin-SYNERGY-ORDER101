package com.synerge.order101.chat.model.service;


import com.synerge.order101.chat.exception.ChatErrorCode;
import com.synerge.order101.chat.model.dto.ChatDto;
import com.synerge.order101.chat.model.entity.ChatRoom;
import com.synerge.order101.chat.model.entity.JoinedChatRoom;
import com.synerge.order101.chat.model.entity.Message;
import com.synerge.order101.chat.model.entity.MessageReadStatus;
import com.synerge.order101.chat.model.repository.ChatMessageRepository;
import com.synerge.order101.chat.model.repository.ChatParticipantRepository;
import com.synerge.order101.chat.model.repository.ChatRoomRepository;
import com.synerge.order101.chat.model.repository.ReadStatusRepository;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.notification.model.service.NotificationService;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void saveMessage(long chatRoomId, ChatDto message) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElseThrow(()->
                new CustomException(ChatErrorCode.CHATROOM_NOT_FOUND));
        // 보낸 사람 조회
        User sender = userRepository.findByName((message.getSenderName())).orElseThrow(() ->
                new CustomException(ChatErrorCode.USER_NOT_FOUND));

        if(sender == null){
            throw new CustomException(ChatErrorCode.USER_NOT_FOUND);
        }
        // 메세지 저장
        Message ms = Message.builder()
                .chatRoom(chatRoom)
                .user(sender)
                .content(message.getMessage())
                .build();
        chatMessageRepository.save(ms);
        // 사용자별로 읽음여부 저장
        List<JoinedChatRoom>  joinedChatRooms = chatParticipantRepository.findByChatRoom(chatRoom);
        for (JoinedChatRoom joinedChatRoom : joinedChatRooms) {
            MessageReadStatus messageReadStatus = MessageReadStatus.builder()
                    .chatRoom(chatRoom)
                    .user(joinedChatRoom.getUser())
                    .message(ms)
                    .isRead(joinedChatRoom.getUser().equals(sender))
                    .build();
            readStatusRepository.save(messageReadStatus);
        }
    }

    // join 객체 생성 후 저장
    public void addParticipantToRoom(ChatRoom chatRoom, User user) {
        JoinedChatRoom chatParticipant = JoinedChatRoom.builder()
                .chatRoom(chatRoom)
                .user(user)
                .build();
        chatParticipantRepository.save(chatParticipant);
    }

    public List<ChatDto> getChatHistory(long roomId, User user) {
        // 내가 해당 채팅방의 참여자가 아닐 경우 에러 발생
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()-> new CustomException(ChatErrorCode.CHATROOM_NOT_FOUND));

        long userId = user.getUserId();

        // 참가자 여부 확인을 userId로
        boolean isParticipant = chatParticipantRepository
                .existsByChatRoom_ChatRoomIdAndUser_UserId(roomId, userId);

        if (!isParticipant) {
            throw new CustomException(ChatErrorCode.NOT_CHAT_MEMBER);
        }
        // 특정 룸에 대한 message 조회
        List<Message> chatMessages = chatMessageRepository.findByChatRoomOrderByCreatedAtAsc(chatRoom);
        List<ChatDto> dtos = new ArrayList<>();
        for (Message m : chatMessages) {
            ChatDto chatDto = ChatDto.builder()
                    .message(m.getContent())
                    .senderName(m.getUser().getName())
                    .createdAt(m.getCreatedAt())
                    .build();
            dtos.add(chatDto);
        }
        return dtos;
    }

    public boolean isRoomParticipant(long userId, long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()-> new CustomException(ChatErrorCode.CHATROOM_NOT_FOUND));

        User user = userRepository.findById(userId).orElseThrow(()->
                new CustomException(ChatErrorCode.USER_NOT_FOUND));

        List<JoinedChatRoom> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        for (JoinedChatRoom c : chatParticipants){
            if(Objects.equals(c.getUser().getUserId(), user.getUserId())){
                return true;
            }
        }
        return false;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int messageRead(long roomId, User user) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()-> new CustomException(ChatErrorCode.CHATROOM_NOT_FOUND));

        long userId = user.getUserId();
        int updated = readStatusRepository.markAsReadIgnore(roomId, userId);
        // updated < 예상치 면 일부 행은 충돌로 스킵된 것. 다음 호출에서 다시 갱신될 수 있음.
        return updated;
    }


    public long getOrCreatePrivateRoom(String otherNickname, User user) {
        // 1. 상대 유저 조회
        User otherUser = userRepository.findByName(otherNickname)
                .orElseThrow(() -> new CustomException(ChatErrorCode.USER_NOT_FOUND));

        // 2. 허용된 조합인지 검사 (지금은 조상원 ↔ 윤석현만 허용)
        if (!isValidPair(user, otherUser)) {
            // 여기서 바로 에러 던짐 → 프론트에 "담당된 가맹점주(HQ)가 존재하지 않습니다." 전달
            throw new CustomException(ChatErrorCode.NOT_ASSIGNED_PARTNER);
        }

        // 3. 기존 1:1 채팅방 있는지 확인
        Optional<ChatRoom> chatRoom =
                chatParticipantRepository.findExistingPrivateRoom(user.getUserId(), otherUser.getUserId());
        if (chatRoom.isPresent()) {
            return chatRoom.get().getChatRoomId();
        }

        // 4. 없으면 새 방 생성
        ChatRoom newRoom = ChatRoom.builder()
                .chatRoomName(user.getName() + "-" + otherUser.getName() + "의 채팅방")
                .build();
        chatRoomRepository.save(newRoom);

        // 두 사람 참가자로 추가
        addParticipantToRoom(newRoom, user);
        addParticipantToRoom(newRoom, otherUser);

        return newRoom.getChatRoomId();
    }

    // 지금은 하드코딩: 나중에 storeId 기반으로만 바꾸면 됨
    private boolean isValidPair(User me, User other) {
        boolean case1 = "조상원".equals(me.getName()) && "윤석현".equals(other.getName());
        boolean case2 = "윤석현".equals(me.getName()) && "조상원".equals(other.getName());
        boolean case3 = "이진일".equals(me.getName()) && "이진삼".equals(other.getName());
        boolean case4 = "이진삼".equals(me.getName()) && "이진일".equals(other.getName());
        boolean case5 = "이진이".equals(me.getName()) && "이진사".equals(other.getName());
        boolean case6 = "이진사".equals(me.getName()) && "이진이".equals(other.getName());
        boolean case7 = "이진구".equals(me.getName()) && "이진오".equals(other.getName());
        boolean case8 = "이진오".equals(me.getName()) && "이진구".equals(other.getName());
        return case1 || case2 || case3 || case4 || case5 || case6 || case7 || case8;
    }


    public void assertAccessible(long roomId, User user) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() ->
                new CustomException(ChatErrorCode.CHATROOM_NOT_FOUND));

        long userId = user.getUserId();

        boolean users = chatParticipantRepository.existsByChatRoom_ChatRoomIdAndUser_UserId(roomId, userId);
        if(!users){ throw new CustomException(ChatErrorCode.NOT_CHAT_MEMBER);}
    }

}
