package com.mia.mini12306.user.mapper;

import com.mia.mini12306.user.UserAccount;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT id, username, password_hash AS passwordHash,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM users
            WHERE username = #{username}
            """)
    UserAccount findByUsername(@Param("username") String username);

    @Select ("""
             SELECT id, username,
                    created_at AS createdAt, updated_at AS updatedAt
             FROM users
             WHERE id = #{id}
             """)
    UserAccount findById(@Param("id") Long id);

    @Insert("""
            INSERT INTO users (username, password_hash)
            VALUES (#{username}, #{passwordHash})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAccount user);
}
