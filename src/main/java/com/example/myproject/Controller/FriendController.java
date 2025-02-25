package com.example.myproject.Controller;

import com.example.myproject.Model.Result;
import com.example.myproject.Model.User;
import com.example.myproject.Service.FriendsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class FriendController {
    // 本应该将其合并到UserController中，但是内容过多不好修改，故将其分开

    @Autowired
    private FriendsService friendsService;

    @GetMapping("/getFriends")
    public Result getFriends(@RequestAttribute("user") User user) {
        return friendsService.getFriends(user.getUserid());
    }

    @PostMapping("/addFriend")
    public Result addFriend(@RequestAttribute("user") User user, @RequestBody Map<String, String> friend) {
        return friendsService.addFriend(user.getUserid(), friend.get("friend"));
    }

}
