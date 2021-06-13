package guru.sfg.beer.order.service.events;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ValidateOrderResultListener {

    public final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESULT)
    public void handleValidateOrderResult(@Payload ValidateOrderResult validateOrderResult){
        log.info("Received Validation order result for beer id",validateOrderResult.getBeerOrderId());
        beerOrderManager.validateBeerOrder(validateOrderResult.getBeerOrderId(),validateOrderResult.getIsValid());
    }


}
