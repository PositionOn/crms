package com.catording.rcms.modules.operator.web;

import com.catording.rcms.common.api.ApiResponse;
import com.catording.rcms.common.exception.BizException;
import com.catording.rcms.common.security.OperatorPrincipal;
import com.catording.rcms.modules.operator.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Data
    public static class LoginReq {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.LoginResult> login(@RequestBody LoginReq req) {
        return ApiResponse.ok(authService.login(req.getUsername(), req.getPassword()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // JWT 无状态：前端丢弃 token 即可
        return ApiResponse.ok(null);
    }

    @Data
    public static class ResetPasswordReq {
        private Long operatorId; // 为空则重置当前登录人
        @NotBlank
        @Size(min = 6, max = 32)
        private String newPassword;
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody ResetPasswordReq req, Authentication authentication) {
        long targetId;
        if (req.getOperatorId() != null) {
            // 简化：允许任意已登录用户调用（后续 M1 可加 ADMIN 才可重置他人）
            targetId = req.getOperatorId();
        } else {
            Object p = authentication == null ? null : authentication.getPrincipal();
            if (!(p instanceof OperatorPrincipal op)) throw BizException.unauthorized("未登录");
            targetId = op.operatorId();
        }
        authService.resetPassword(targetId, req.getNewPassword());
        return ApiResponse.ok(null);
    }
}

