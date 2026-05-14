package com.ithuangma.java.ai.langchain4j.Bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginForm {
    private String username;
    private String password;
    private String captcha;
}
