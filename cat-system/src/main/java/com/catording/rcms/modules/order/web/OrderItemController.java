package com.catording.rcms.modules.order.web;

import com.catording.rcms.common.api.ApiResponse;
import com.catording.rcms.modules.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderItemController {
    private final OrderService orderService;

    public OrderItemController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 兼容计划路由：POST /api/orders/{id}/items
    @Data
    public static class AddReq {
        @NotNull
        private Long dishId;
        @NotNull
        private Integer quantity;
    }

    @PostMapping("/api/orders/{id}/items")
    public ApiResponse<Long> add(@PathVariable("id") long orderId, @RequestBody @Valid AddReq req) {
        return ApiResponse.ok(orderService.addItem(orderId, req.getDishId(), req.getQuantity()));
    }

    // 兼容计划路由：PUT /api/order-items/{itemId}
    @Data
    public static class UpdateReq {
        @NotNull
        private Integer quantity;
    }

    @PutMapping("/api/order-items/{itemId}")
    public ApiResponse<Void> update(@PathVariable long itemId, @RequestBody @Valid UpdateReq req) {
        orderService.updateItem(itemId, req.getQuantity());
        return ApiResponse.ok(null);
    }

    // 兼容计划路由：DELETE /api/order-items/{itemId}
    @DeleteMapping("/api/order-items/{itemId}")
    public ApiResponse<Void> delete(@PathVariable long itemId) {
        orderService.deleteItem(itemId);
        return ApiResponse.ok(null);
    }
}

