package guru.sfg.beer.order.service.statemachine;


import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocationExceptionAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext) {
        String beerOrderId = String.valueOf(stateContext.getMessage().getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER));
        log.info("#### Compensating transaction for Allocation Exception : {}",beerOrderId);
        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_EXCEPTION_QUEUE,beerOrderId);
    }
}
