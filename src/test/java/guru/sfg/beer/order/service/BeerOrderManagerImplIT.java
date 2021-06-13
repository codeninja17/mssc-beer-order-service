package guru.sfg.beer.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.beer.order.service.services.beer.BeerServiceImpl;
import guru.sfg.brewery.model.BeerDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(WireMockExtension.class)
@SpringBootTest
public class BeerOrderManagerImplIT {

    @Autowired
    BeerOrderManager beerOrderManager;

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    WireMockServer wireMockServer;

    @Autowired
    ObjectMapper objectMapper;

    Customer testCustomer;

    UUID beerId = UUID.randomUUID();

    @Autowired
    JmsTemplate jmsTemplate;

    @TestConfiguration
    static class RestTemplateBuilderProvider{

        @Bean(destroyMethod = "stop")
        public WireMockServer wireMockServer(){
            WireMockServer wireMockServer = with(wireMockConfig().port(8083));
            wireMockServer.start();
            return wireMockServer;
        }

    }

    @BeforeEach
    public void setUp(){
        testCustomer = customerRepository.save(Customer.builder().customerName("Test Customer").build());
    }

    @Test
    void testNewToAllocatedOrder() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1+beerDto.getUpc()).willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
        BeerOrder beerOrder = createBeerOrder();
        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATED,foundOrder.getOrderStatus());

        });
        BeerOrder savedBeerOrder2 = beerOrderRepository.findById(savedBeerOrder.getId()).get();
        Assertions.assertNotNull(savedBeerOrder);
        assertEquals(BeerOrderStatusEnum.ALLOCATED,savedBeerOrder2.getOrderStatus());
        savedBeerOrder2.getBeerOrderLines().forEach(beerOrderLine -> {
            assertEquals(beerOrderLine.getQuantityAllocated(),beerOrderLine.getOrderQuantity());
        });

    }

    @Test
    void testFailedOrderValidation() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1+beerDto.getUpc())
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("FAILED_VAL_REF");
        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.VALIDATION_EXCEPTION,foundOrder.getOrderStatus());

        });
        BeerOrder savedBeerOrder2 = beerOrderRepository.findById(savedBeerOrder.getId()).get();
        assertEquals(BeerOrderStatusEnum.VALIDATION_EXCEPTION,savedBeerOrder2.getOrderStatus());
    }

    @Test
    void testOrderFailedAllocation() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1+beerDto.getUpc())
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("FAILED_ALLOCATION_REF");
        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATION_EXCEPTION,foundOrder.getOrderStatus());

        });
        UUID beerOrderId = UUID.fromString((String) jmsTemplate.receiveAndConvert(JmsConfig.ALLOCATE_EXCEPTION_QUEUE));

        BeerOrder savedBeerOrder2 = beerOrderRepository.findById(beerOrderId).get();
        assertEquals(BeerOrderStatusEnum.ALLOCATION_EXCEPTION,savedBeerOrder2.getOrderStatus());
    }

    @Test
    void testOrderPartialAllocation() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1+beerDto.getUpc())
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("PARTIAL_ALLOCATION_REF");
        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.PENDING_INVENTORY,foundOrder.getOrderStatus());

        });

        UUID beerOrderId = UUID.fromString((String) jmsTemplate.receiveAndConvert(JmsConfig.ALLOCATE_EXCEPTION_QUEUE));

        BeerOrder savedBeerOrder2 = beerOrderRepository.findById(beerOrderId).get();
        assertEquals(BeerOrderStatusEnum.PENDING_INVENTORY,savedBeerOrder2.getOrderStatus());
    }

    @Test
    void testNewToPickedUpOrder() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1+beerDto.getUpc()).willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
        BeerOrder beerOrder = createBeerOrder();
        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATED,foundOrder.getOrderStatus());

        });

        beerOrderManager.beerOrderPickedUp(savedBeerOrder.getId());

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.PICKED_UP,foundOrder.getOrderStatus());

        });


        BeerOrder savedBeerOrder2 = beerOrderRepository.findById(savedBeerOrder.getId()).get();
        assertEquals(BeerOrderStatusEnum.PICKED_UP,savedBeerOrder2.getOrderStatus());
    }

    private BeerOrder createBeerOrder(){
        BeerOrder beerOrder = BeerOrder.builder().customer(testCustomer).build();
        Set<BeerOrderLine> beerOrderLineSet = new HashSet<>();
        beerOrderLineSet.add(BeerOrderLine.builder().beerId(beerId).upc("12345").orderQuantity(1).beerOrder(beerOrder).build());
        beerOrder.setBeerOrderLines(beerOrderLineSet);
        return beerOrder;
    }

}
