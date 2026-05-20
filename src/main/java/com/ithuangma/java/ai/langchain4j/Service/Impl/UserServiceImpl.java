package com.ithuangma.java.ai.langchain4j.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ithuangma.java.ai.langchain4j.Service.UserService;
import com.ithuangma.java.ai.langchain4j.common.ErrorCode;
import com.ithuangma.java.ai.langchain4j.entity.User;
import com.ithuangma.java.ai.langchain4j.exception.BusinessException;
import com.ithuangma.java.ai.langchain4j.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String SALT = "songsong";

    @Override
    public User login(String username, String password) {
        // 校验参数非空
        if (StringUtils.isAnyBlank(username, password)) {
            throw new BusinessException("用户名和密码不能为空", ErrorCode.PARAMS_ERROR.getCode(), "登录参数为空");
        }
            
        // 验证用户名长度
        if (username.length() < 2) {
            throw new BusinessException("用户名至少2个字符", ErrorCode.PARAMS_ERROR.getCode(), "用户名长度不足");
        }
            
        // 验证密码长度
        if (password.length() < 6) {
            throw new BusinessException("密码至少6个字符", ErrorCode.PARAMS_ERROR.getCode(), "密码长度不足");
        }
            
        // 验证用户名不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(username);
        if (matcher.find()) {
            throw new BusinessException("用户名只能包含字母、数字和下划线", ErrorCode.PARAMS_ERROR.getCode(), "用户名包含非法字符");
        }
            
        // 对输入的密码进行加密
        String encryptedInputPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
            
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = getOne(queryWrapper);
        
        // 为了安全，不泄露用户是否存在，统一返回用户名或密码错误
        if (user == null) {
            throw new BusinessException("用户名或密码错误", ErrorCode.AUTH_ERROR.getCode(), "用户不存在");
        }
        
        // 比较加密后的密码
        if (!user.getPassword().equals(encryptedInputPassword)) {
            throw new BusinessException("用户名或密码错误", ErrorCode.AUTH_ERROR.getCode(), "密码不正确");
        }
        return user;
    }

    @Override
    public User register(String username, String password, String phone, String email) {
        // 校验参数非空
        if (StringUtils.isAnyBlank(username, password, phone, email)) {
            throw new BusinessException("所有字段都不能为空", ErrorCode.PARAMS_ERROR.getCode(), "注册参数为空");
        }
            
        // 验证用户名长度
        if (username.length() < 2) {
            throw new BusinessException("用户名至少2个字符", ErrorCode.PARAMS_ERROR.getCode(), "用户名长度不足");
        }
            
        // 验证密码长度
        if (password.length() < 6) {
            throw new BusinessException("密码至少6个字符", ErrorCode.PARAMS_ERROR.getCode(), "密码长度不足");
        }
            
        // 验证手机号和邮箱格式
        if (phone != null && !isValidPhone(phone)) {
            throw new BusinessException("手机号格式不正确", ErrorCode.PARAMS_ERROR.getCode(), "无效的手机号格式");
        }
            
        if (email != null && !isValidEmail(email)) {
            throw new BusinessException("邮箱格式不正确", ErrorCode.PARAMS_ERROR.getCode(), "无效的邮箱格式");
        }
            
        // 验证用户名不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(username);
        if (matcher.find()) {
            throw new BusinessException("用户名只能包含字母、数字和下划线", ErrorCode.PARAMS_ERROR.getCode(), "用户名包含非法字符");
        }
            
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User existUser = getOne(queryWrapper);
        if (existUser != null) {
            throw new BusinessException("该用户名已被注册", ErrorCode.PARAMS_ERROR.getCode(), "用户名重复");
        }
            
        // 对密码进行加密
        String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        log.info("SALT: {}", SALT);
        log.info("原始密码：{}", password);
        log.info("加密后的密码：{}", encryptedPassword);
            
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(encryptedPassword);
        newUser.setPhone(phone);
        newUser.setEmail(email);
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());
        save(newUser);
        return newUser;
    }
    
    /**
     * 验证手机号格式
     */
    private boolean isValidPhone(String phone) {
        String phoneRegex = "^1[3-9]\\d{9}$";
        return phone.matches(phoneRegex);
    }
    
    /**
     * 验证邮箱格式
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }
}