package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.statemachine.BeerOrderStateMachineInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static final String ORDER_ID_HEADER = "order_id";
    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateMachineInterceptor beerOrderStateMachineInterceptor;

    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);
        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);
        return savedBeerOrder;
    }

    @Override
    public BeerOrder validateBeerOrder(UUID beerOrderId, Boolean isValid){
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderId);
        if(isValid){
            sendBeerOrderEvent(beerOrder,BeerOrderEventEnum.VALIDATION_PASSED);

            BeerOrder validatedBeerOrder = beerOrderRepository.findOneById(beerOrderId);

            sendBeerOrderEvent(validatedBeerOrder,BeerOrderEventEnum.ALLOCATE_ORDER);

        }else{
            sendBeerOrderEvent(beerOrder,BeerOrderEventEnum.VALIDATION_FAILED);
        }
        return beerOrder;
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum beerOrderEventEnum){
        StateMachine<BeerOrderStatusEnum,BeerOrderEventEnum> sm = build(beerOrder);
        Message message = MessageBuilder.withPayload(beerOrderEventEnum)
                .setHeader(ORDER_ID_HEADER,beerOrder.getId().toString())
                .build();
        sm.sendEvent(message);
    }

    private StateMachine<BeerOrderStatusEnum,BeerOrderEventEnum> build(BeerOrder beerOrder){
        StateMachine<BeerOrderStatusEnum,BeerOrderEventEnum> sm = stateMachineFactory.getStateMachine(beerOrder.getId());
        sm.stop();
        sm.getStateMachineAccessor().doWithAllRegions(beerOrderStatusEnumBeerOrderEventEnumStateMachineAccess -> {
            beerOrderStatusEnumBeerOrderEventEnumStateMachineAccess.addStateMachineInterceptor(beerOrderStateMachineInterceptor);
            beerOrderStatusEnumBeerOrderEventEnumStateMachineAccess.resetStateMachine(new DefaultStateMachineContext<>(
                    beerOrder.getOrderStatus(),null,null,null
            ));
        });

        sm.start();

        return sm;
    }
}
