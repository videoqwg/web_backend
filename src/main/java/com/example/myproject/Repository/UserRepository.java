package com.example.myproject.Repository;

import com.example.myproject.Model.User;
import org.apache.ibatis.annotations.*;



@Mapper
public interface UserRepository {
    //根据用户名查找用户
    @Select("select * from user where userid=#{userid}")
    User findUser(String userid);
    //插入用户信息，有用户名、密码、创建时间、更新时间
    @Insert("insert into user(userid,username,password,phone,created_at,updated_at) values(#{username},#{username},#{password},#{username},now(),now())")
    void addUser(User user);

    @Update("UPDATE user SET avatar = #{avatar} WHERE userid = #{userid}")
    void updateAvatar(@Param("userid") String userid, @Param("avatar") String avatar);

    @Update("UPDATE user SET username = #{username}, phone = #{phone}, email = #{mail}, introduction = #{introduction} WHERE userid = #{userid}")
    void updateUserData(
            @Param("userid") String userid,
            @Param("username") String username,
            @Param("phone") String phone,
            @Param("mail") String mail,
            @Param("introduction") String introduction
    );

    @Update("UPDATE user SET username = #{username} WHERE userid = #{userid}")
    void updateUserName(@Param("userid") String userid, @Param("username") String username);

    @Update("UPDATE user SET password = #{password} WHERE userid = #{userid}")
    void updateUserPassword(@Param("userid") String userid, @Param("password") String password);

}
