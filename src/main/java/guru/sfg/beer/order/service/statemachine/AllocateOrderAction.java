package guru.sfg.beer.order.service.statemachine;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import guru.sfg.brewery.model.events.AllocateBeerOrderRequest;
import guru.sfg.brewery.model.events.ValidateBeerOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final BeerOrderRepository beerOrderRepository;
    private final JmsTemplate jmsTemplate;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext) {
            String beerOrderId = String.valueOf(stateContext.getMessage().getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER));
            BeerOrder beerOrder = beerOrderRepository.getOne(UUID.fromString(beerOrderId));
            jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER, AllocateBeerOrderRequest
                    .builder().beerOrderDto(beerOrderMapper.beerOrderToDto(beerOrder)).build());
            log.debug("Sent Allocation Request for beer Order ID : {}" , beerOrderId);
        }
}

