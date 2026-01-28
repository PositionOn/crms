package com.catording.rcms.modules.order.web;

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
@RequestMapping("/api/orders")
public class OrderCashierCompatController {
    private final OrderService orderService;

    public OrderCashierCompatController(OrderService orderService) {
        this.orderService = orderService;
    }

    private static OperatorPrincipal principal(Authentication a) {
        Object p = a == null ? null : a.getPrincipal();
        if (!(p instanceof OperatorPrincipal op)) throw BizException.unauthorized("未登录");
        return op;
    }

    // 兼容计划路由：POST /api/orders/{id}/apply-discount
    @Data
    public static class ApplyDiscountReq {
        private Long ruleId;
        @DecimalMin("0.00")
        private BigDecimal manualDiscountAmount;
        private String reason;
    }

    @PostMapping("/{id}/apply-discount")
    public ApiResponse<ConsumeOrderEntity> applyDiscount(@PathVariable("id") long orderId, @RequestBody @Valid ApplyDiscountReq req) {
        return ApiResponse.ok(orderService.applyDiscount(orderId, req.getRuleId(), req.getManualDiscountAmount(), req.getReason()));
    }

    // 兼容计划路由：POST /api/orders/{id}/checkout
    @Data
    public static class CheckoutReq {
        @NotBlank
        private String method;
        @NotNull
        @DecimalMin("0.01")
        private BigDecimal payAmount;
    }

    @PostMapping("/{id}/checkout")
    public ApiResponse<Void> checkout(@PathVariable("id") long orderId, @RequestBody @Valid CheckoutReq req, Authentication auth) {
        OperatorPrincipal op = principal(auth);
        orderService.checkout(orderId, req.getMethod(), req.getPayAmount(), op.operatorId());
        return ApiResponse.ok(null);
    }

    // 兼容计划路由：GET /api/orders/{id}/receipt
    @GetMapping("/{id}/receipt")
    public ApiResponse<OrderService.OrderDetail> receipt(@PathVariable("id") long orderId) {
        return ApiResponse.ok(orderService.getOrderDetail(orderId));
    }
}

