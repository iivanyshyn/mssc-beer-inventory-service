package com.ivn.msscbeerinventoryservice.services;

import com.ivn.brewery.model.events.AllocateOrderRequest;
import com.ivn.brewery.model.events.AllocateOrderResult;
import com.ivn.msscbeerinventoryservice.config.JmsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocationListener {

    private final AllocationService allocationService;
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(AllocateOrderRequest request) {
        AllocateOrderResult.AllocateOrderResultBuilder allocateOrderResultBuilder = AllocateOrderResult.builder()
                .beerOrder(request.getBeerOrder());
        try {
            Boolean allocationResult = allocationService.allocateOrder(request.getBeerOrder());

            if (allocationResult) {
                allocateOrderResultBuilder.pendingInventory(false);
            } else {
                allocateOrderResultBuilder.pendingInventory(true);
            }
        } catch (Exception e) {
            log.error("Allocation failed for Order Id:" + request.getBeerOrder().getId());
            allocateOrderResultBuilder.allocationError(true);
        }

        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE, allocateOrderResultBuilder.build());

    }
}
