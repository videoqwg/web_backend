package com.example.myproject.Repository;

import com.example.myproject.Model.Friends;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FriendsRepository extends MongoRepository<Friends, String> {
    Friends findByUserId(String userId);
}

