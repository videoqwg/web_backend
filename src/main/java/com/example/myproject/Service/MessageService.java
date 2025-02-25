package com.example.myproject.Service;

import com.example.myproject.Model.MessageDTO;
import com.example.myproject.Model.Result;

import java.util.List;
import java.util.Map;

public interface MessageService {
    Result sendMessage(MessageDTO message);
    Result sendGroupMessage(MessageDTO message);
    Map<String, List<MessageDTO>> processMessages(String senderId);
    Result syncMessages(String timestamp, String userId, String commandType);
}
