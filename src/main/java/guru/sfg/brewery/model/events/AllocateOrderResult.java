package guru.sfg.brewery.model.events;

import guru.sfg.brewery.model.BeerOrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Created by jt on 12/3/19.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocateOrderResult implements Serializable {

    private static final long serialVersionUID = -4606670886122374367L;

    private BeerOrderDto beerOrderDto;
    private Boolean allocationError = false;
    private Boolean pendingInventory = false;
}