package com.ivn.msscbeerinventoryservice.services;

import com.ivn.brewery.model.BeerOrderDto;

public interface AllocationService {

    Boolean allocateOrder(BeerOrderDto beerOrderDto);

    void deallocateOrder(BeerOrderDto beerOrder);
}
