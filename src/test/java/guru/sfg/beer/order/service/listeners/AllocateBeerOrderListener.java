package guru.sfg.beer.order.service.listeners;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.AllocateBeerOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import guru.sfg.brewery.model.events.ValidateBeerOrderRequest;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocateBeerOrderListener {

    final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER)
    public void listen(Message msg){
        AllocateBeerOrderRequest allocateBeerOrderRequest = (AllocateBeerOrderRequest) msg.getPayload();

        allocateBeerOrderRequest.getBeerOrderDto().getBeerOrderLines().stream().forEach(beerOrderLineDto -> {
            beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
        });

        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE, AllocateOrderResult.builder()
                .beerOrderDto(allocateBeerOrderRequest.getBeerOrderDto()).pendingInventory(false).allocationError(false).build());
        log.info("Allocated beer order for integration testing");
    }


}
