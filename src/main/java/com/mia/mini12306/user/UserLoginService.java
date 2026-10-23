package com.mia.mini12306.user;

import com.mia.mini12306.user.dto.UserProfileResponse;
import com.mia.mini12306.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class UserLoginService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserLoginService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserProfileResponse authenticate(
            String username,
            String password
    ) {
        if(password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidCredentialsException();
        }

        UserAccount user = userMapper.findByUsername(username);

        if(user == null ||
                !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return UserProfileResponse.from(user);
    }

    public UserProfileResponse getCurrentUser(Long userId) {
        UserAccount user = userMapper.findById(userId);

        if(user == null) {
            throw new NotLoggedInException();
        }

        return UserProfileResponse.from(user);
    }
}
