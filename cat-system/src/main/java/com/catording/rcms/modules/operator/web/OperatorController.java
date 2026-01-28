package com.catording.rcms.modules.operator.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.catording.rcms.common.api.ApiResponse;
import com.catording.rcms.common.exception.BizException;
import com.catording.rcms.modules.operator.entity.OperatorEntity;
import com.catording.rcms.modules.operator.mapper.OperatorMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operators")
public class OperatorController {
    private final OperatorMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public OperatorController(OperatorMapper mapper, PasswordEncoder passwordEncoder) {
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ApiResponse<List<OperatorEntity>> list(@RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(mapper.selectList(new LambdaQueryWrapper<OperatorEntity>()
                .eq(status != null && !status.isBlank(), OperatorEntity::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(OperatorEntity::getUsername, keyword)
                        .or()
                        .like(OperatorEntity::getName, keyword))
                .orderByDesc(OperatorEntity::getId)));
    }

    @Data
    public static class CreateReq {
        @NotBlank
        private String username;
        @NotBlank
        @Size(min = 6, max = 32)
        private String password;
        @NotBlank
        private String name;
        @NotBlank
        private String role; // ADMIN/CASHIER
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid CreateReq req) {
        OperatorEntity e = new OperatorEntity();
        e.setUsername(req.getUsername().trim());
        e.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        e.setName(req.getName().trim());
        e.setRole(req.getRole());
        e.setStatus("ENABLED");
        try {
            mapper.insert(e);
        } catch (Exception ex) {
            throw BizException.badRequest("用户名已存在");
        }
        return ApiResponse.ok(e.getId());
    }

    @Data
    public static class UpdateReq {
        @NotBlank
        private String name;
        @NotBlank
        private String status; // ENABLED/DISABLED
        @NotBlank
        private String role; // ADMIN/CASHIER
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @RequestBody @Valid UpdateReq req) {
        OperatorEntity e = mapper.selectById(id);
        if (e == null) throw BizException.badRequest("操作员不存在");
        e.setName(req.getName().trim());
        e.setStatus(req.getStatus());
        e.setRole(req.getRole());
        mapper.updateById(e);
        return ApiResponse.ok(null);
    }

    @Data
    public static class ResetPwdReq {
        @NotNull
        private Long operatorId;
        @NotBlank
        @Size(min = 6, max = 32)
        private String newPassword;
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody @Valid ResetPwdReq req) {
        OperatorEntity e = mapper.selectById(req.getOperatorId());
        if (e == null) throw BizException.badRequest("操作员不存在");
        e.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        mapper.updateById(e);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        if (id == 1L) throw BizException.badRequest("默认管理员不可删除");
        mapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}

