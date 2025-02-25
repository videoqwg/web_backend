package com.example.myproject.Service.Impl;

import com.example.myproject.Model.Friends;
import com.example.myproject.Model.Result;
import com.example.myproject.Model.User;
import com.example.myproject.Repository.FriendsRepository;
import com.example.myproject.Repository.UserRepository;
import com.example.myproject.Service.FriendsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class FriendsServiceImpl implements FriendsService {
    @Autowired
    private FriendsRepository friendsRepository;

    @Autowired
    private UserRepository userRepository;


    @Override
    public boolean validateUser(String friendId) {
        try {
            User user = userRepository.findUser(friendId);
            return user != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    @Override
    public Result getFriends(String userId) {
        try {
            List<User> friends = new ArrayList<>();
            List<Friends.Friend> friendList = friendsRepository.findByUserId(userId).getFriends();
            for (Friends.Friend friend : friendList) {
                User user = userRepository.findUser(friend.getFriendId());
                friends.add(user);
            }
            return Result.success(friends);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("获取好友列表失败");
        }
    }

    @Override
    @Transactional // 添加事务管理，确保数据一致性
    public Result addFriend(String userId, String friendId) {
        try {
            // 验证好友是否存在
            if (!validateUser(friendId)) {
                return Result.failure("用户不存在");
            }

            // 获取用户的好友列表
            Friends userFriends = friendsRepository.findByUserId(userId);
            Friends friendFriends = friendsRepository.findByUserId(friendId);

            if (userFriends == null) {
                userFriends = new Friends(userId);
            }

            if (friendFriends == null) {
                friendFriends = new Friends(friendId);
            }

            // 检查用户是否已是好友
            for (Friends.Friend friend : userFriends.getFriends()) {
                if (friend.getFriendId().equals(friendId)) {
                    return Result.failure("已经添加该好友");
                }
            }

            // 添加好友关系到用户的好友列表中
            Friends.Friend userToFriend = new Friends.Friend();
            userToFriend.setFriendId(friendId);
            userToFriend.setStatus("accepted"); // 用户发起请求为 pending
            userToFriend.setCreatedAt(LocalDateTime.now());

            // 添加好友关系到好友的好友列表中
            Friends.Friend friendToUser = new Friends.Friend();
            friendToUser.setFriendId(userId);
            friendToUser.setStatus("accepted"); // 好友收到请求状态也为 pending
            friendToUser.setCreatedAt(LocalDateTime.now());

            // 更新双方的好友列表
            userFriends.getFriends().add(userToFriend);
            friendFriends.getFriends().add(friendToUser);

            // 保存到数据库
            friendsRepository.save(userFriends);
            friendsRepository.save(friendFriends);
            Map<String,User> data = Map.of("friend",userRepository.findUser(friendId));
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("添加好友失败"+e.getMessage());
        }
    }

    // 更新好友状态
    @Override
    @Transactional
    public Result updateFriendStatus(String userId, String friendId, String status) {
        Friends user = friendsRepository.findByUserId(userId);
        Friends friend = friendsRepository.findByUserId(friendId);
        if (user != null && friend != null) {
            try {
                user.getFriends().stream()
                        .filter(friend_temp -> friend_temp.getFriendId().equals(friendId))
                        .findFirst()
                        .ifPresent(friend_temp -> friend_temp.setStatus(status));

                friend.getFriends().stream()
                        .filter(friend_temp -> friend_temp.getFriendId().equals(friendId))
                        .findFirst()
                        .ifPresent(friend_temp -> friend_temp.setStatus(status));

                friendsRepository.save(user);
                friendsRepository.save(friend);
                return Result.success();
            } catch (Exception e) {
                e.printStackTrace();
                return Result.failure("更新好友状态失败"+e.getMessage());
            }
        }
        return Result.failure("更新好友状态失败，好友不存在");
    }

    // 删除好友
    public Result removeFriend(String userId, String friendId) {
        Friends user = friendsRepository.findByUserId(userId);
        Friends friend = friendsRepository.findByUserId(friendId);
        if (user != null && friend != null) {
            try {
                user.getFriends().removeIf(friend_temp -> friend_temp.getFriendId().equals(friendId));
                friend.getFriends().removeIf(friend_temp -> friend_temp.getFriendId().equals(userId));

                friendsRepository.save(user);
                friendsRepository.save(friend);

                return Result.success();
            } catch (Exception e) {
                e.printStackTrace();
                return Result.failure("删除好友失败"+e.getMessage());
            }
        }else {
            return Result.failure("删除好友失败，好友不存在");
        }
    }
}

