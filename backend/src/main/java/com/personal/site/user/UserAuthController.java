package com.personal.site.user;

import com.personal.site.content.ContentModels;
import com.personal.site.content.MessageRateLimiter;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/auth")
public class UserAuthController {
    private final UserAuthService auth;
    private final MessageRateLimiter rateLimiter;

    public UserAuthController(UserAuthService auth, MessageRateLimiter rateLimiter) {
        this.auth = auth;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/code")
    public ContentModels.AuthCodeResult sendCode(@Valid @RequestBody ContentModels.AuthCodeInput input, HttpServletRequest request) {
        rateLimiter.checkInteraction(request.getRemoteAddr());
        return auth.sendCode(input);
    }

    @PostMapping("/login")
    public ContentModels.UserAuthResponse login(@Valid @RequestBody ContentModels.AuthVerifyInput input) {
        return auth.verifyAndAuthenticate(input);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) auth.logout(authorization.substring(7).trim());
    }

    @GetMapping("/me")
    public ContentModels.UserView me(HttpServletRequest request) {
        return auth.currentUser(request);
    }
}
