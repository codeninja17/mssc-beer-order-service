package guru.sfg.beer.order.service.listeners;


import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.ValidateBeerOrderRequest;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class ValidateBeerOrderListener {

    final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER)
    public void listen(Message msg){
        ValidateBeerOrderRequest validateBeerOrderRequest = (ValidateBeerOrderRequest) msg.getPayload();
        boolean isValid = true;

        if(validateBeerOrderRequest.getBeerOrderDto().getCustomerRef()!=null
                && validateBeerOrderRequest.getBeerOrderDto().getCustomerRef().equals("FAILED_VAL_REF")) {
                isValid = false;
            }

        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESULT, ValidateOrderResult.builder().isValid(isValid).
                beerOrderId(validateBeerOrderRequest.getBeerOrderDto().getId()).build());

        log.info("Validated beer order for integration testing");

    }

}
