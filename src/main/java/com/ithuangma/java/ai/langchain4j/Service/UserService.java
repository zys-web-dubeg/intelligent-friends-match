package com.ithuangma.java.ai.langchain4j.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ithuangma.java.ai.langchain4j.entity.User;

public interface UserService extends IService<User> {
    User login(String username, String password);
    User register(String username, String password, String phone, String email);
}
