package com.example.myproject.Service.Impl;

import com.example.myproject.Model.*;
import com.example.myproject.Repository.FriendsRepository;
import com.example.myproject.Repository.GroupsRepository;
import com.example.myproject.Repository.UserRepository;
import com.example.myproject.Service.UserService;
import com.example.myproject.Util.JwtUtil;
import com.example.myproject.Manager.DynamicListenerManager;
import io.jsonwebtoken.Claims;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;



@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendsRepository friendsRepository;

    @Autowired
    private GroupsRepository groupsRepository;

    @Autowired
    private AmqpAdmin rabbitAdmin; // 即 RabbitAdmin

    @Autowired
    private TopicExchange instructionExchange;

    @Autowired
    private TopicExchange groupExchange;

    @Autowired
    private DynamicListenerManager dynamicListenerManager;

    @Autowired
    private JwtUtil jwtUtil;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String UPLOAD_DIRECTORY = "D:/Desktop/vue/vue-admin-template-master/src/assets"; // 文件保存目录

    @Override
    public User findUser(String username) {
        User user = userRepository.findUser(username);
        return user;
    }

    @Override
    public Result register(String username, String password) {
        String avatar = "/api/user/getAvatar/" + username + ".png";
        String encodedPassword = passwordEncoder.encode(password);
        Friends friends = new Friends(username);
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setAvatar(avatar);
        createDefaultAvatar(username);
        userRepository.addUser(user);
        friendsRepository.save(friends);
        return Result.success();
    }

    private void createDefaultAvatar(String userid)  {
        String sourcePath = UPLOAD_DIRECTORY + "/default.png";
        String targetPath = UPLOAD_DIRECTORY + "/" + userid + ".png";
        try {
            Files.copy(Paths.get(sourcePath), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Result login(String userid, String password) {
        User user = userRepository.findUser(userid);
        // 1. 查数据库中 userId 所在的群列表
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            List<String> roles = new ArrayList<>();
            roles.add(user.getRole());
            String token = jwtUtil.generateToken(user.getUserid(), user.getUsername(), user.getAvatar(), roles);
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);


            // 动态声明队列 & Binding
            createUserQueueAndBinding(userid);

            // 启动监听容器
            dynamicListenerManager.startListenerForUser(userid);


            return Result.success(data);
        } else {
            return Result.failure("用户名或密码错误");
        }
    }

    /**
     * 为用户创建队列并绑定到 Exchange
     */
    private void createUserQueueAndBinding(String userId) {
        // 这里采用动态绑定方便后续进行扩展

        String queueName = UserService.getQueueNameByUserId(userId);
        List<Groups> groups = groupsRepository.findByMembersUserId(userId);

        // 声明队列(若已存在，不会重复创建)
        org.springframework.amqp.core.Queue userQueue = new org.springframework.amqp.core.Queue(queueName, true, false, false);
        rabbitAdmin.declareQueue(userQueue);

        // 声明 Binding
        String routingKey = "user." + userId;
        Binding binding = BindingBuilder.bind(userQueue)
                .to(instructionExchange)
                .with(routingKey);

        // 对每个群做一次声明绑定
        for (Groups group : groups) {
            String groupRK = "group." + group.getGroupId() + ".*";
            rabbitAdmin.declareBinding(
                    BindingBuilder.bind(new org.springframework.amqp.core.Queue(queueName, true))
                            .to(groupExchange)
                            .with(groupRK)
            );
        }

        rabbitAdmin.declareBinding(binding);
    }

    @Override
    public Result info(String userid) {
        Map<String, Object> info = new HashMap<>();
        User user = findUser(userid);
        List<String> roles = new ArrayList<>();
        roles.add(user.getRole());
        if (user != null) {
            info.put("userid", user.getUserid());
            info.put("username", user.getUsername());
            info.put("avatar", user.getAvatar());
            info.put("roles", roles);
            info.put("phone", user.getPhone());
            info.put("email", user.getEmail());
            info.put("introduction", user.getIntroduction());
            return Result.success(info);
        } else {
            return Result.failure("用户不存在");
        }
    }

    @Override
    public Result logout(String userid) {
        dynamicListenerManager.stopListenerForUser(userid);
        return Result.success();
    }

    @Override
    public Result getRoles(User user) {
        List<String> roles = new ArrayList<>();
        roles.add(user.getRole());
        return Result.success(roles);
    }

    // 更新用户头像
    @Override
    public Result updateAvatar(String userid, MultipartFile avatar) {
        try {
            // 确保保存目录存在
            File uploadDir = new File(UPLOAD_DIRECTORY);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs(); // 创建保存目录
            }

            // 获取文件原始名，并构造保存路径
            String originalFilename = avatar.getOriginalFilename();
            String avtarFilename = "/api/user/getAvatar/" + originalFilename;
            File saveFile = new File(uploadDir, originalFilename);

            // 保存文件到指定路径
            avatar.transferTo(saveFile);
            userRepository.updateAvatar(userid, avtarFilename);
            Map<String, Object> avatarPath = new HashMap<>();
            avatarPath.put("avatar", avtarFilename);
            return Result.success(avatarPath);
        } catch (Exception e) {
            return Result.failure("上传失败");
        }
    }

    @Override
    public ResponseEntity<Resource> getAvatar(String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIRECTORY).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public Result updateUserData(Map<String, String> userForm, User user) {
        String username = userForm.get("name");
        String phone = userForm.get("phone");
        String email = userForm.get("email");
        String introduction = userForm.get("introduction");
        try {
            userRepository.updateUserData(user.getUserid(), username, phone, email, introduction);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("修改失败");
        }
        return Result.success();
    }

    @Override
    public Result updateAccountData(Map<String, String> userForm, User user) {
        String username = userForm.get("username");
        if (userForm.containsKey("password")) {
            String password = userForm.get("password");
            String encodedPassword = passwordEncoder.encode(password);
            try {
                userRepository.updateUserName(user.getUserid(), username);
                userRepository.updateUserPassword(user.getUserid(), encodedPassword);
                return Result.success();
            } catch (Exception e) {
                e.printStackTrace();
                return Result.failure("修改失败");
            }
        } else {
            try {
                userRepository.updateUserName(user.getUserid(), username);
                return Result.success();
            } catch (Exception e) {
                e.printStackTrace();
                return Result.failure("修改失败");
            }
        }
    }
}
