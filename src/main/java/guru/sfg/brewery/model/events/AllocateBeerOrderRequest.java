package guru.sfg.brewery.model.events;

import guru.sfg.brewery.model.BeerOrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocateBeerOrderRequest implements Serializable {

    private static final long serialVersionUID = -2024598498688710739L;
    private BeerOrderDto beerOrderDto;

}
