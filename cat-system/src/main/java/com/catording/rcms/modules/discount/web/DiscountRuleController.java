package com.catording.rcms.modules.discount.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.catording.rcms.common.api.ApiResponse;
import com.catording.rcms.common.exception.BizException;
import com.catording.rcms.modules.discount.entity.DiscountRuleEntity;
import com.catording.rcms.modules.discount.mapper.DiscountRuleMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discount-rules")
public class DiscountRuleController {
    private final DiscountRuleMapper mapper;

    public DiscountRuleController(DiscountRuleMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping
    public ApiResponse<List<DiscountRuleEntity>> list(@RequestParam(required = false) Integer enabled) {
        return ApiResponse.ok(mapper.selectList(new LambdaQueryWrapper<DiscountRuleEntity>()
                .eq(enabled != null, DiscountRuleEntity::getEnabled, enabled)
                .orderByDesc(DiscountRuleEntity::getPriority)
                .orderByDesc(DiscountRuleEntity::getId)));
    }

    @Data
    public static class CreateReq {
        @NotBlank
        private String name;
        @NotBlank
        private String type;
        @NotBlank
        private String paramsJson;
        @NotNull
        private Integer priority;
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid CreateReq req) {
        DiscountRuleEntity e = new DiscountRuleEntity();
        e.setName(req.getName().trim());
        e.setType(req.getType());
        e.setParamsJson(req.getParamsJson());
        e.setEnabled(1);
        e.setPriority(req.getPriority());
        mapper.insert(e);
        return ApiResponse.ok(e.getId());
    }

    @Data
    public static class UpdateReq {
        @NotBlank
        private String name;
        @NotBlank
        private String type;
        @NotBlank
        private String paramsJson;
        @NotNull
        private Integer enabled;
        @NotNull
        private Integer priority;
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @RequestBody @Valid UpdateReq req) {
        DiscountRuleEntity e = mapper.selectById(id);
        if (e == null) throw BizException.badRequest("规则不存在");
        e.setName(req.getName().trim());
        e.setType(req.getType());
        e.setParamsJson(req.getParamsJson());
        e.setEnabled(req.getEnabled());
        e.setPriority(req.getPriority());
        mapper.updateById(e);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable long id) {
        DiscountRuleEntity e = mapper.selectById(id);
        if (e == null) throw BizException.badRequest("规则不存在");
        e.setEnabled(1);
        mapper.updateById(e);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable long id) {
        DiscountRuleEntity e = mapper.selectById(id);
        if (e == null) throw BizException.badRequest("规则不存在");
        e.setEnabled(0);
        mapper.updateById(e);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        mapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}

