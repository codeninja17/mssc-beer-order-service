package guru.sfg.beer.order.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@Configuration
@EnableJms
public class JmsConfig {

    public static final String VALIDATE_ORDER = "validate-order";
    public static final String VALIDATE_ORDER_RESULT = "validate-order-result";
    public static final String ALLOCATE_ORDER = "allocate-order";
    public static final String ALLOCATE_ORDER_RESPONSE_QUEUE = "allocate-order-response";
    public static final String ALLOCATE_EXCEPTION_QUEUE = "allocate-exception-queue";
    public static final String DEALLOCATE_ORDER_QUEUE = "deallocate-order" ;


    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper){
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setObjectMapper(objectMapper);
        return converter;
    }
}
