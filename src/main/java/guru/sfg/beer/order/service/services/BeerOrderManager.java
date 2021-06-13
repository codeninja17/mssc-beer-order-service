package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.brewery.model.BeerOrderDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface BeerOrderManager {

    BeerOrder newBeerOrder(BeerOrder beerOrder);


    BeerOrder validateBeerOrder(UUID beerOrderId, Boolean isValid);

    void beerOrderAllocationPassed(BeerOrderDto beerOrder);

    @Transactional
    void beerOrderPickedUp(UUID beerOrderId);

    void beerOrderAllocationPendingInventory(BeerOrderDto beerOrder);

    void beerOrderAllocationFailed(BeerOrderDto beerOrder);
}
