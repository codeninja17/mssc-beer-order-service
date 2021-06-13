package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.statemachine.BeerOrderStateMachineInterceptor;
import guru.sfg.brewery.model.BeerOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static final String ORDER_ID_HEADER = "order_id";
    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateMachineInterceptor beerOrderStateMachineInterceptor;

    @Override
    @Transactional
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);
        BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Override
    @Transactional
    public BeerOrder validateBeerOrder(UUID beerOrderId, Boolean isValid){
        BeerOrder beerOrder = beerOrderRepository.findById(beerOrderId).get();
        if(isValid){
            sendBeerOrderEvent(beerOrder,BeerOrderEventEnum.VALIDATION_PASSED);
            Optional<BeerOrder> validatedBeerOrder = beerOrderRepository.findById(beerOrderId);
            validatedBeerOrder.ifPresentOrElse(beerOrder1 -> {
                sendBeerOrderEvent(beerOrder1,BeerOrderEventEnum.ALLOCATE_ORDER);
            },()->log.info("Beer Object Not Found"));
        }else{
            sendBeerOrderEvent(beerOrder,BeerOrderEventEnum.VALIDATION_FAILED);
        }
        return beerOrder;
    }

    @Transactional
    @Override
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
        Optional<BeerOrder> beerOrder = beerOrderRepository.findById(beerOrderDto.getId());
        beerOrder.ifPresentOrElse(beerOrder1 -> {
            sendBeerOrderEvent(beerOrder1, BeerOrderEventEnum.ALLOCATION_SUCCESS);
            updateAllocatedQty(beerOrderDto);
        },()->log.info("Beer Object Not Found"));

    }

    @Transactional
    @Override
    public void beerOrderPickedUp(UUID beerOrderId) {
        Optional<BeerOrder> beerOrder = beerOrderRepository.findById(beerOrderId);
        beerOrder.ifPresentOrElse(beerOrder1 -> {
            sendBeerOrderEvent(beerOrder1, BeerOrderEventEnum.BEER_ORDER_PICKED_UP);
        },()->log.info("Beer Object Not Found"));
    }

    @Override
    @Transactional
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.findById(beerOrderDto.getId()).get();
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);
        updateAllocatedQty(beerOrderDto);
    }

    private void updateAllocatedQty(BeerOrderDto beerOrderDto) {
        BeerOrder allocatedOrder = beerOrderRepository.findById(beerOrderDto.getId()).get();

        allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
            beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                if(beerOrderLine.getId() .equals(beerOrderLineDto.getId())){
                    beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                }
            });
        });

        beerOrderRepository.saveAndFlush(allocatedOrder);
    }

    @Override
    @Transactional
    public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED);
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
