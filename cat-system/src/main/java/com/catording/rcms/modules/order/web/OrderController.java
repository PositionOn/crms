package com.catording.rcms.modules.order.web;

import com.catording.rcms.common.api.ApiResponse;
import com.catording.rcms.modules.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderService.OrderDetail> detail(@PathVariable("id") long orderId) {
        return ApiResponse.ok(orderService.getOrderDetail(orderId));
    }
}

