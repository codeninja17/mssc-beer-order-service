package guru.sfg.brewery.model.events;

import guru.sfg.brewery.model.BeerOrderDto;
import lombok.*;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateBeerOrderRequest implements Serializable {

    private static final long serialVersionUID = -1010050915378861537L;
    private BeerOrderDto beerOrderDto;

}
