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

@Slf4j
@RequiredArgsConstructor
@Component
public class ValidateBeerOrderListener {

    final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER)
    public void listen(Message msg){
        ValidateBeerOrderRequest validateBeerOrderRequest = (ValidateBeerOrderRequest) msg.getPayload();

        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESULT, ValidateOrderResult.builder().isValid(true).
                beerOrderId(validateBeerOrderRequest.getBeerOrderDto().getId()).build());

        log.info("Validated beer order for integration testing");

    }

}
