package com.ivn.msscbeerinventoryservice.services;

import com.ivn.brewery.model.BeerOrderDto;
import com.ivn.brewery.model.BeerOrderLineDto;
import com.ivn.msscbeerinventoryservice.domain.BeerInventory;
import com.ivn.msscbeerinventoryservice.repositories.BeerInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationServiceImpl implements AllocationService {

    private final BeerInventoryRepository beerInventoryRepository;

    @Override
    public Boolean allocateOrder(BeerOrderDto beerOrderDto) {
        log.debug("Allocating OrderId: " + beerOrderDto.getId());

        AtomicInteger totalOrdered = new AtomicInteger();
        AtomicInteger totalAllocated = new AtomicInteger();

        beerOrderDto.getBeerOrderLines().forEach(beerOrderLine -> {
            if (((beerOrderLine.getOrderQuantity() != null ? beerOrderLine.getOrderQuantity() : 0)
                    - (beerOrderLine.getQuantityAllocated() != null ? beerOrderLine.getQuantityAllocated() : 0)) > 0) {
                allocateBeerOrderLine(beerOrderLine);
            }
            totalOrdered.set(totalAllocated.get() + beerOrderLine.getOrderQuantity());
            totalAllocated.set(totalAllocated.get() + (beerOrderLine.getQuantityAllocated() != null ? beerOrderLine.getQuantityAllocated() : 0));
        });

        log.debug("Total Ordered: " + totalOrdered.get() + " Total Allocated: " + totalAllocated.get());

        return totalOrdered.get() == totalAllocated.get();
    }

    private void allocateBeerOrderLine(BeerOrderLineDto beerOrderLine) {
        List<BeerInventory> beerInventories = beerInventoryRepository.findAllByUpc(beerOrderLine.getUpc());

        beerInventories.forEach(beerInventory -> {
            int inventory = (beerInventory.getQuantityOnHand() == null) ? 0 : beerInventory.getQuantityOnHand();
            int orderQuantity = (beerOrderLine.getOrderQuantity() == null) ? 0 : beerOrderLine.getOrderQuantity();
            int allocatedQuantity = (beerOrderLine.getQuantityAllocated() == null) ? 0 : beerOrderLine.getQuantityAllocated();
            int quantityToAllocate = orderQuantity - allocatedQuantity;

            if (inventory >= quantityToAllocate) {
                inventory = inventory - quantityToAllocate;
                beerOrderLine.setQuantityAllocated(orderQuantity);
                beerInventory.setQuantityOnHand(inventory);

                beerInventoryRepository.save(beerInventory);
            } else if (inventory > 0) {
                beerOrderLine.setQuantityAllocated(allocatedQuantity + inventory);
                beerInventory.setQuantityOnHand(0);

                beerInventoryRepository.delete(beerInventory);
            }
        });
    }
}
