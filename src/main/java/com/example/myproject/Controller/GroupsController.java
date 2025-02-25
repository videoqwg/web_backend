package com.example.myproject.Controller;

import com.example.myproject.Model.Result;
import com.example.myproject.Model.User;
import com.example.myproject.Service.GroupsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class GroupsController {
    @Autowired
    private GroupsService groupsService;

    @GetMapping("/getGroups")
    public Result getGroups(@RequestAttribute("user") User user) {
        return groupsService.getGroups(user.getUserid());
    }

    @PostMapping("/createGroup")
    public Result createGroup(@RequestBody Map<String, String> group, @RequestAttribute("user") User user) {
        return groupsService.initGroups(user.getUserid(), group.get("groupName"));
    }

    @PostMapping("/addMember")
    public Result addMember(@RequestBody Map<String, String> member) {
        return groupsService.addMember(member.get("groupId"), member.get("memberId"));
    }

    // 修改成员状态
    @PutMapping("/updateMember")
    public Result updateMember(@RequestBody Map<String, String> member) {
        return groupsService.updateMemberStatus(member.get("groupId"), member.get("memberId"), member.get("status"));
    }


}
