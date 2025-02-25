package com.example.myproject.Controller;

import com.example.myproject.Model.MessageDTO;
import com.example.myproject.Model.Result;
import com.example.myproject.Model.User;
import com.example.myproject.Service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.myproject.Service.UserService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
// 实现用户登录及用户注册功能
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;


    // 这里的username其实是id，不是真正的用户名
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        //先查询用户是否存在，如果存在则返回错误信息，不存在则注册
        if (userService.findUser(user.getUsername()) != null) {
            return Result.failure("用户已存在");
        } else {
            return userService.register(user.getUsername(), user.getPassword());
        }
    }
    // 这里的username其实是id，不是真正的用户名
    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        if (userService.findUser(user.getUsername()) == null) {
            return Result.failure("用户不存在");
        } else {
            return userService.login(user.getUsername(), user.getPassword());
        }
    }

    @GetMapping("/info")
    public Result info(@RequestAttribute("user") User user) {
        return userService.info(user.getUserid());
    }

    @PostMapping("/logout")
    public Result logout(@RequestAttribute("user") User user) {
        return userService.logout(user.getUserid());
    }

    @GetMapping("/getRoles")
    public Result getRoles(@RequestAttribute("user") User user) {
        return userService.getRoles(user);
    }


    @PostMapping(value = "/uploadAvatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadAvatar(HttpServletRequest request, @RequestAttribute("user") User user) {
        // 将请求转换为 MultipartHttpServletRequest
        MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;

        // 获取所有字段名和对应的文件
        Map<String, MultipartFile> fileMap = multiRequest.getFileMap();
        // 校验文件数量
        if (fileMap.isEmpty()) {
            return Result.failure("文件为空");
        } else if (fileMap.size() > 1) {
            return Result.failure("只能上传一个文件");
        }
        // 获取唯一的文件
        Map.Entry<String, MultipartFile> entry = fileMap.entrySet().iterator().next();
        String userId = user.getUserid();
        MultipartFile avatar = entry.getValue();
        return userService.updateAvatar(userId, avatar);
    }

    @GetMapping("/getAvatar/{filename}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        return userService.getAvatar(filename);
    }

    @PostMapping("/updateUserData")
    public Result updateUserData(@RequestBody Map<String, String> userForm, @RequestAttribute("user") User user) {
        return userService.updateUserData(userForm, user);
    }
    @PostMapping("/updateAccountData")
    public Result updateAccountData(@RequestBody Map<String, String> userForm, @RequestAttribute("user") User user) {
        return userService.updateAccountData(userForm, user);
    }

    @PostMapping("/sendCmd")
    public Result sendCommand(@RequestBody MessageDTO instruction) {
        return messageService.sendMessage(instruction);
    }

    @PostMapping("/sendGroupCmd")
    public Result sendGroupCommand(@RequestBody MessageDTO instruction) {
        return messageService.sendGroupMessage(instruction);
    }

    @GetMapping("/loadMessages")
    public Result loadMessages(@RequestAttribute("user") User user){
        return Result.success(messageService.processMessages(user.getUserid()));
    }

    @PostMapping("/getFriendInfo")
    public Result getFriendInfo(@RequestBody Map<String,String> friend){
        return userService.info(friend.get("friendId"));
    }

    @PostMapping("/syncMessages")
    public Result syncMessages(@RequestBody Map<String,String> timestamp, @RequestAttribute("user") User user){
        return messageService.syncMessages(timestamp.get("timestamp"),user.getUserid(),"chat");
    }

    @PostMapping("/syncNotifications")
    public Result syncNotifications(@RequestBody Map<String,String> timestamp, @RequestAttribute("user") User user){
        return messageService.syncMessages(timestamp.get("timestamp"),user.getUserid(),"notification");
    }

    @GetMapping("/test")
    public Result test() {
        return Result.success();
    }

}
