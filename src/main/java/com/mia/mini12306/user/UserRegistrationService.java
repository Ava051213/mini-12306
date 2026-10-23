package com.mia.mini12306.user;

import com.mia.mini12306.user.mapper.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount register(String username, String password) {
        if(userMapper.findByUsername(username) != null) {
            throw new UserAlreadyExistsException(username);
        }

        UserAccount user = new UserAccount(
                username,
                passwordEncoder.encode(password)
        );

        try {
            userMapper.insert(user);
        } catch(DuplicateKeyException exception) {
            throw new UserAlreadyExistsException(username);
        }

        return userMapper.findByUsername(username);
    }
}
