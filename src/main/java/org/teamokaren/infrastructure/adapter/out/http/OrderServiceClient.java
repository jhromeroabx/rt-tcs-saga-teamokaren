package org.teamokaren.infrastructure.adapter.out.http;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "orderServiceClient",
        url = "${clients.order-service.url}",
        path = "/api/orders")
public interface OrderServiceClient {

    @PostMapping
    OrderServiceResponse createOrder(@RequestBody OrderServiceCreateRequest request);

    @PostMapping("/{orderId}/cancel")
    void cancelOrder(@PathVariable("orderId") String orderId, @RequestBody OrderServiceCancelRequest request);
}