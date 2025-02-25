package com.example.myproject.Service.Impl;

import com.example.myproject.Model.Groups;
import com.example.myproject.Model.Result;
import com.example.myproject.Model.User;
import com.example.myproject.Repository.GroupsRepository;
import com.example.myproject.Repository.UserRepository;
import com.example.myproject.Service.GroupsService;
import com.example.myproject.Service.SequenceGeneratorService;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GroupsServiceImpl implements GroupsService {
    @Autowired
    private GroupsRepository groupsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SequenceGeneratorService sequenceGeneratorService;

    @Autowired
    private RabbitAdmin rabbitAdmin;
    @Autowired
    private TopicExchange groupExchange;

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

    public Result initGroups(String userId, String groupName) {
        try {
            Groups groups = new Groups();

            groups.setGroupId(sequenceGeneratorService.getNextSequence("groupId"));
            groups.setUserId(userId);
            groups.setGroupName(groupName);
            groups.setMembers(new ArrayList<>());
            groups.setCreatedAt(LocalDateTime.now());

            groups.setMembers(new ArrayList<>());
            Groups.Member member = new Groups.Member();
            member.setUserId(userId);
            member.setRole("member");
            member.setStatus("pending");
            member.setJoinedAt(LocalDateTime.now());
            groups.getMembers().add(member);

            joinGroup(userId,groups.getGroupId());

            groupsRepository.save(groups);
            return Result.success(groups);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("初始化群组失败");
        }
    }

    // 添加群组成员
    public Result addMember(String groupId, String userId) {
        Groups group = groupsRepository.findByGroupId(groupId);
        if(!validateUser(userId)) {
            return Result.failure("用户不存在");
        }

        Groups.Member member = new Groups.Member();
        member.setUserId(userId);
        member.setRole("member");
        member.setStatus("pending");
        member.setJoinedAt(LocalDateTime.now());

        joinGroup(userId, groupId);

        group.getMembers().add(member);
        groupsRepository.save(group);

        return Result.success();
    }

    // 删除群组成员
    public Result removeMember(String groupId, String userId) {
        Groups group = groupsRepository.findByGroupId(groupId);
        if(!validateUser(userId)) {
            return Result.failure("用户不存在");
        }
        if (group != null) {
            group.getMembers().removeIf(member -> member.getUserId().equals(userId));
            groupsRepository.save(group);
            return Result.success();
        }
        return Result.failure("删除群组成员失败");
    }

    // 更新群成员状态
    public Result updateMemberStatus(String groupId, String userId, String status) {
        Groups group = groupsRepository.findByGroupId(groupId);
        if(!validateUser(userId)) {
            return Result.failure("用户不存在");
        }
        if (group != null) {
            if(status.equals("accepted")) {
                joinGroup(userId, groupId);
                group.getMembers().stream()
                        .filter(member -> member.getUserId().equals(userId))
                        .findFirst()
                        .ifPresent(member -> member.setStatus(status));
                groupsRepository.save(group);
            } else if(status.equals("deleted")) {
                leaveGroup(userId, groupId);
                removeMember(groupId, userId);
            }else {
                group.getMembers().stream()
                        .filter(member -> member.getUserId().equals(userId))
                        .findFirst()
                        .ifPresent(member -> member.setStatus(status));
                groupsRepository.save(group);
            }

            return Result.success();
        }
        return Result.failure("更新群组成员状态失败");
    }

    // 更新群组成员角色
    public Result updateMemberRole(String groupId, String userId, String role) {
        Groups group = groupsRepository.findByGroupId(groupId);
        if(!validateUser(userId)) {
            return Result.failure("用户不存在");
        }
        if (group != null) {
            group.getMembers().stream()
                    .filter(member -> member.getUserId().equals(userId))
                    .findFirst()
                    .ifPresent(member -> member.setRole(role));
            groupsRepository.save(group);
            return Result.success();
        }
        return Result.failure("更新群组成员角色失败");
    }

    // 查询群组成员
    /*
    public Result getMembers(String groupId) {
        try {
            List<User> members = new ArrayList<>();
            List<Groups.Member> memberList = groupsRepository.findByGroupId(groupId).getMembers();
            for (Groups.Member member : memberList) {
                User user = userRepository.findUser(member.getUserId());
                members.add(user);
            }
            return Result.success(members);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("获取群组成员失败");
        }
    }
    */

    // 查询用户所在的群组
    public Result getGroups(String userId) {
        try {
            List<Groups> groups = groupsRepository.findByMembersUserId(userId);
            return Result.success(groups);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("获取群组列表失败");
        }
    }

    public Result updateGroupName(String groupId, String groupName) {
        try {
            Groups group = groupsRepository.findByGroupId(groupId);
            group.setGroupName(groupName);
            groupsRepository.save(group);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("修改群组名称失败");
        }
    }

    // 删除群组
    public Result deleteGroup(String groupId) {
        try {
            groupsRepository.deleteById(groupId);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("删除群组失败");
        }
    }

    /**
     * 用户加入群组
     *  - 为其队列绑定 "group.<groupId>.<userId>"
     */
    private void joinGroup(String userId, String groupId) {
        // 用户队列与群消息交换机绑定
        String queueName = "queue.user." + userId;
        String routingKey = "group." + groupId + ".*";

        Binding binding = BindingBuilder.bind(new org.springframework.amqp.core.Queue(queueName, true))
                .to(groupExchange)
                .with(routingKey);
        rabbitAdmin.declareBinding(binding);

        System.out.println("用户 " + userId + " 加入群 " + groupId +
                "，绑定RoutingKey=" + routingKey);
    }

    /**
     * 用户退出群组（如需解绑）
     */
    private void leaveGroup(String userId, String groupId) {
        String queueName = "queue.user." + userId;
        String routingKey = "group." + groupId + "." + userId;

        // RabbitAdmin 没有直接提供 removeBinding 的方法，需要自行构造 Binding 再执行 remove
        Binding binding = new Binding(queueName,
                Binding.DestinationType.QUEUE,
                groupExchange.getName(),
                routingKey,
                null);
        rabbitAdmin.removeBinding(binding);

        System.out.println("用户 " + userId + " 退出群 " + groupId +
                "，解绑RoutingKey=" + routingKey);
    }
}
