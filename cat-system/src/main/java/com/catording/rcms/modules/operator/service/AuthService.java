package com.catording.rcms.modules.operator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.catording.rcms.common.exception.BizException;
import com.catording.rcms.common.security.JwtTokenService;
import com.catording.rcms.modules.operator.entity.OperatorEntity;
import com.catording.rcms.modules.operator.mapper.OperatorMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {
    private final OperatorMapper operatorMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(OperatorMapper operatorMapper, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.operatorMapper = operatorMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Data
    @AllArgsConstructor
    public static class LoginResult {
        private String token;
        private Long operatorId;
        private String operatorName;
        private String role;
    }

    @Transactional
    public LoginResult login(String username, String password) {
        OperatorEntity op = operatorMapper.selectOne(new LambdaQueryWrapper<OperatorEntity>()
                .eq(OperatorEntity::getUsername, username));
        if (op == null) throw BizException.unauthorized("账号或密码错误");
        if (!"ENABLED".equalsIgnoreCase(op.getStatus())) throw BizException.forbidden("账号已停用");

        String stored = op.getPasswordHash();
        boolean ok;
        if (stored != null && stored.startsWith("$2")) {
            ok = passwordEncoder.matches(password, stored);
        } else {
            ok = password.equals(stored);
            if (ok) {
                op.setPasswordHash(passwordEncoder.encode(password));
                operatorMapper.updateById(op);
            }
        }
        if (!ok) throw BizException.unauthorized("账号或密码错误");

        op.setLastLoginAt(LocalDateTime.now());
        operatorMapper.updateById(op);

        String token = jwtTokenService.createToken(op.getId(), op.getUsername(), op.getRole());
        return new LoginResult(token, op.getId(), op.getName(), op.getRole());
    }

    @Transactional
    public void resetPassword(long operatorId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw BizException.badRequest("新密码至少 6 位");
        }
        OperatorEntity op = operatorMapper.selectById(operatorId);
        if (op == null) throw BizException.badRequest("操作员不存在");
        op.setPasswordHash(passwordEncoder.encode(newPassword));
        operatorMapper.updateById(op);
    }
}

