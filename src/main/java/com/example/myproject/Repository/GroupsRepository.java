package com.example.myproject.Repository;

import com.example.myproject.Model.Groups;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface GroupsRepository extends MongoRepository<Groups, String> {
    Groups findByGroupId(String groupId);
    List<Groups> findByMembersUserId(String userId);

}
