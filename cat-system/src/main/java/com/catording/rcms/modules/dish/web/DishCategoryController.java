package com.catording.rcms.modules.dish.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.catording.rcms.common.api.ApiResponse;
import com.catording.rcms.common.exception.BizException;
import com.catording.rcms.modules.dish.entity.DishCategoryEntity;
import com.catording.rcms.modules.dish.mapper.DishCategoryMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dish-categories")
public class DishCategoryController {
    private final DishCategoryMapper mapper;

    public DishCategoryController(DishCategoryMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping
    public ApiResponse<List<DishCategoryEntity>> list() {
        return ApiResponse.ok(mapper.selectList(new LambdaQueryWrapper<DishCategoryEntity>()
                .orderByAsc(DishCategoryEntity::getSort)
                .orderByAsc(DishCategoryEntity::getId)));
    }

    @Data
    public static class CreateReq {
        @NotBlank
        private String name;
        @NotNull
        @Min(0)
        private Integer sort;
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid CreateReq req) {
        DishCategoryEntity e = new DishCategoryEntity();
        e.setName(req.getName().trim());
        e.setSort(req.getSort());
        e.setStatus("ENABLED");
        try {
            mapper.insert(e);
        } catch (Exception ex) {
            throw BizException.badRequest("分类名称已存在");
        }
        return ApiResponse.ok(e.getId());
    }

    @Data
    public static class UpdateReq {
        @NotBlank
        private String name;
        @NotNull
        @Min(0)
        private Integer sort;
        @NotBlank
        private String status; // ENABLED/DISABLED
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @RequestBody @Valid UpdateReq req) {
        DishCategoryEntity e = mapper.selectById(id);
        if (e == null) throw BizException.badRequest("分类不存在");
        e.setName(req.getName().trim());
        e.setSort(req.getSort());
        e.setStatus(req.getStatus());
        try {
            mapper.updateById(e);
        } catch (Exception ex) {
            throw BizException.badRequest("分类名称已存在");
        }
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        mapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}

