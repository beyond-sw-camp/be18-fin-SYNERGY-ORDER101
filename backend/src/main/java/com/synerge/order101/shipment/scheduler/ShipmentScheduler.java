package com.synerge.order101.shipment.scheduler;

import com.synerge.order101.shipment.model.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentScheduler {
    private final ShipmentService shipmentService;

    @Scheduled(cron = "0 * * * * *")
    public void run(){
        log.debug("Start Shipment Status Update Scheduler");
        shipmentService.updateShipmentStatus();
        log.debug("End Shipment Status Update Scheduler");
    }
}
