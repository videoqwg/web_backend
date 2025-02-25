package com.example.myproject.Service;

import com.example.myproject.Model.Result;

public interface GroupsService {
    boolean validateUser(String friendId);
    Result initGroups(String userId, String groupName);
    Result addMember(String groupId, String userId);
    Result removeMember(String groupId, String userId);
    Result updateMemberStatus(String groupId, String userId, String status);
    Result getGroups(String userId);
    Result updateMemberRole(String groupId, String userId, String role);
    Result updateGroupName(String groupId, String groupName);
    Result deleteGroup(String groupId);
}
