package com.catording.rcms.modules.dish.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.catording.rcms.common.api.ApiResponse;
import com.catording.rcms.common.exception.BizException;
import com.catording.rcms.modules.dish.entity.DishEntity;
import com.catording.rcms.modules.dish.mapper.DishMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/dishes")
public class DishController {
    private final DishMapper mapper;

    public DishController(DishMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping
    public ApiResponse<List<DishEntity>> list(@RequestParam(required = false) Long categoryId,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(mapper.selectList(new LambdaQueryWrapper<DishEntity>()
                .eq(categoryId != null, DishEntity::getCategoryId, categoryId)
                .eq(status != null && !status.isBlank(), DishEntity::getStatus, status)
                .like(keyword != null && !keyword.isBlank(), DishEntity::getName, keyword)
                .orderByDesc(DishEntity::getId)));
    }

    @Data
    public static class CreateReq {
        @NotBlank
        private String name;
        @NotNull
        private Long categoryId;
        @NotNull
        @DecimalMin("0.01")
        private BigDecimal price;
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid CreateReq req) {
        DishEntity e = new DishEntity();
        e.setName(req.getName().trim());
        e.setCategoryId(req.getCategoryId());
        e.setPrice(req.getPrice());
        e.setStatus("ON");
        mapper.insert(e);
        return ApiResponse.ok(e.getId());
    }

    @Data
    public static class UpdateReq {
        @NotBlank
        private String name;
        @NotNull
        private Long categoryId;
        @NotNull
        @DecimalMin("0.01")
        private BigDecimal price;
        @NotBlank
        private String status; // ON/OFF
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @RequestBody @Valid UpdateReq req) {
        DishEntity e = mapper.selectById(id);
        if (e == null) throw BizException.badRequest("菜品不存在");
        e.setName(req.getName().trim());
        e.setCategoryId(req.getCategoryId());
        e.setPrice(req.getPrice());
        e.setStatus(req.getStatus());
        mapper.updateById(e);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        mapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    @Data
    public static class BatchPriceReq {
        @NotNull
        private List<Long> ids;
        @NotNull
        @DecimalMin("0.01")
        private BigDecimal newPrice;
    }

    @PostMapping("/batch-price")
    public ApiResponse<Void> batchPrice(@RequestBody @Valid BatchPriceReq req) {
        if (req.getIds() == null || req.getIds().isEmpty()) throw BizException.badRequest("ids 不能为空");
        for (Long id : req.getIds()) {
            DishEntity e = mapper.selectById(id);
            if (e == null) continue;
            e.setPrice(req.getNewPrice());
            mapper.updateById(e);
        }
        return ApiResponse.ok(null);
    }

    @Data
    public static class BatchStatusReq {
        @NotNull
        private List<Long> ids;
        @NotBlank
        private String status; // ON/OFF
    }

    @PostMapping("/batch-status")
    public ApiResponse<Void> batchStatus(@RequestBody @Valid BatchStatusReq req) {
        if (req.getIds() == null || req.getIds().isEmpty()) throw BizException.badRequest("ids 不能为空");
        for (Long id : req.getIds()) {
            DishEntity e = mapper.selectById(id);
            if (e == null) continue;
            e.setStatus(req.getStatus());
            mapper.updateById(e);
        }
        return ApiResponse.ok(null);
    }
}

