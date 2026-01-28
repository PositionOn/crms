package com.catording.rcms.modules.cashier.web;

import com.catording.rcms.common.api.ApiResponse;
import com.catording.rcms.common.exception.BizException;
import com.catording.rcms.common.security.OperatorPrincipal;
import com.catording.rcms.modules.order.entity.ConsumeOrderEntity;
import com.catording.rcms.modules.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cashier")
public class CashierController {
    private final OrderService orderService;

    public CashierController(OrderService orderService) {
        this.orderService = orderService;
    }

    private static OperatorPrincipal principal(Authentication a) {
        Object p = a == null ? null : a.getPrincipal();
        if (!(p instanceof OperatorPrincipal op)) throw BizException.unauthorized("未登录");
        return op;
    }

    @Data
    public static class OpenTableReq {
        @NotNull
        private Long tableId;
    }

    @PostMapping("/open-table")
    public ApiResponse<ConsumeOrderEntity> open(@RequestBody @Valid OpenTableReq req, Authentication auth) {
        OperatorPrincipal op = principal(auth);
        return ApiResponse.ok(orderService.openTable(req.getTableId(), op.operatorId()));
    }

    @Data
    public static class CancelOpenReq {
        @NotNull
        private Long orderId;
    }

    @PostMapping("/cancel-open")
    public ApiResponse<Void> cancelOpen(@RequestBody @Valid CancelOpenReq req) {
        orderService.cancelOpen(req.getOrderId());
        return ApiResponse.ok(null);
    }

    @Data
    public static class AddItemReq {
        @NotNull
        private Long dishId;
        @NotNull
        private Integer quantity;
    }

    @PostMapping("/orders/{id}/items")
    public ApiResponse<Long> addItem(@PathVariable("id") long orderId, @RequestBody @Valid AddItemReq req) {
        return ApiResponse.ok(orderService.addItem(orderId, req.getDishId(), req.getQuantity()));
    }

    @Data
    public static class UpdateItemReq {
        @NotNull
        private Integer quantity;
    }

    @PutMapping("/order-items/{itemId}")
    public ApiResponse<Void> updateItem(@PathVariable long itemId, @RequestBody @Valid UpdateItemReq req) {
        orderService.updateItem(itemId, req.getQuantity());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/order-items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable long itemId) {
        orderService.deleteItem(itemId);
        return ApiResponse.ok(null);
    }

    @Data
    public static class ApplyDiscountReq {
        private Long ruleId;
        @DecimalMin("0.00")
        private BigDecimal manualDiscountAmount;
        private String reason;
    }

    @PostMapping("/orders/{id}/apply-discount")
    public ApiResponse<ConsumeOrderEntity> applyDiscount(@PathVariable("id") long orderId, @RequestBody @Valid ApplyDiscountReq req) {
        return ApiResponse.ok(orderService.applyDiscount(orderId, req.getRuleId(), req.getManualDiscountAmount(), req.getReason()));
    }

    @Data
    public static class CheckoutReq {
        @NotBlank
        private String method;
        @NotNull
        @DecimalMin("0.01")
        private BigDecimal payAmount;
    }

    @PostMapping("/orders/{id}/checkout")
    public ApiResponse<Void> checkout(@PathVariable("id") long orderId, @RequestBody @Valid CheckoutReq req, Authentication auth) {
        OperatorPrincipal op = principal(auth);
        orderService.checkout(orderId, req.getMethod(), req.getPayAmount(), op.operatorId());
        return ApiResponse.ok(null);
    }

    @GetMapping("/orders/{id}/receipt")
    public ApiResponse<OrderService.OrderDetail> receipt(@PathVariable("id") long orderId) {
        return ApiResponse.ok(orderService.getOrderDetail(orderId));
    }
}

