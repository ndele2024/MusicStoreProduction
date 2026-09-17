package com.musicstore.mapper;

import com.musicstore.domain.User;
import com.musicstore.dto.UserDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getFullName(),
                user.getAge(),
                user.getSex(),
                user.getUserName(),
                user.getUserEmail(),
                user.getRole().toApi(),
                user.getAvatar(),
                user.getPreferences() == null ? List.of() : List.copyOf(user.getPreferences()));
    }
}
