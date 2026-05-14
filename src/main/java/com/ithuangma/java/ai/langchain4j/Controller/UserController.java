package com.ithuangma.java.ai.langchain4j.Controller;

import com.ithuangma.java.ai.langchain4j.Bean.LoginForm;
import com.ithuangma.java.ai.langchain4j.Bean.RegisterForm;
import com.ithuangma.java.ai.langchain4j.Bean.Result;
import com.ithuangma.java.ai.langchain4j.Service.StatisticsService;
import com.ithuangma.java.ai.langchain4j.Service.UserService;
import com.ithuangma.java.ai.langchain4j.entity.User;
import com.ithuangma.java.ai.langchain4j.util.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户认证")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private StatisticsService statisticsService;

    private static final String CAPTCHA_KEY = "captcha";
    private static final int CAPTCHA_LENGTH = 4;
    private static final String CHAR_SET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    @GetMapping("/captcha")
    @Operation(summary = "获取验证码图片")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("image/jpeg");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);

        // 生成验证码
        String captcha = generateCaptcha();
        HttpSession session = request.getSession();
        session.setAttribute(CAPTCHA_KEY, captcha);

        // 生成验证码图片
        BufferedImage image = generateCaptchaImage(captcha);
        ImageIO.write(image, "jpeg", response.getOutputStream());
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<Map<String, Object>> login(@RequestBody LoginForm loginForm, HttpServletRequest request) {
        // 验证验证码
        HttpSession session = request.getSession();
        String sessionCaptcha = (String) session.getAttribute(CAPTCHA_KEY);
        if (sessionCaptcha == null || !sessionCaptcha.equals(loginForm.getCaptcha())) {
            return Result.fail(400, "验证码错误");
        }
        session.removeAttribute(CAPTCHA_KEY);

        User user = userService.login(loginForm.getUsername(), loginForm.getPassword());
        if (user == null) {
            return Result.fail(401, "用户名或密码错误");
        }
        String token = JwtUtils.generateToken(user.getId(), user.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return Result.ok(data);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<User> register(@RequestBody RegisterForm registerForm) {
        User user = userService.register(
                registerForm.getUsername(),
                registerForm.getPassword(),
                registerForm.getPhone(),
                registerForm.getEmail()
        );

        // 记录用户注册统计
        statisticsService.recordUserRegistration();

        return Result.ok(user);
    }

    private String generateCaptcha() {
        Random random = new Random();
        StringBuilder captcha = new StringBuilder();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            int index = random.nextInt(CHAR_SET.length());
            captcha.append(CHAR_SET.charAt(index));
        }
        return captcha.toString();
    }

    private BufferedImage generateCaptchaImage(String captcha) {
        int width = 120;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置背景
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // 设置字体
        g.setFont(new Font("Arial", Font.BOLD, 28));

        // 绘制验证码
        for (int i = 0; i < captcha.length(); i++) {
            g.setColor(new Color(
                    new Random().nextInt(100),
                    new Random().nextInt(100),
                    new Random().nextInt(100)
            ));
            g.drawString(String.valueOf(captcha.charAt(i)), 20 * i + 15, 30);
        }

        // 添加干扰线
        Random random = new Random();
        g.setColor(Color.GRAY);
        for (int i = 0; i < 5; i++) {
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);
            g.drawLine(x1, y1, x2, y2);
        }

        g.dispose();
        return image;
    }
}