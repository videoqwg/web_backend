package com.example.myproject.Service;

import com.example.myproject.Model.Result;

public interface FriendsService {
    boolean validateUser(String friendId);
    Result getFriends(String userId);
    Result addFriend(String userId, String friendId);
    Result removeFriend(String userId, String friendId);
    Result updateFriendStatus(String userId, String friendId, String status);
}
