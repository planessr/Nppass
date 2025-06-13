package com.admin.service;

import com.admin.common.dto.ChangePasswordDto;
import com.admin.common.dto.LoginDto;
import com.admin.common.dto.PageDto;
import com.admin.common.dto.UserDto;
import com.admin.common.dto.UserUpdateDto;
import com.admin.common.lang.R;
import com.admin.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author QAQ
 * @since 2025-06-03
 */
public interface UserService extends IService<User> {

    R login(LoginDto loginDto);

    R createUser(UserDto userDto);

    R getAllUsers(PageDto pageDto);

    R updateUser(UserUpdateDto userUpdateDto);

    R deleteUser(Long id);

    R getUserPackageInfo();
    
    R updatePassword(ChangePasswordDto changePasswordDto);
}
