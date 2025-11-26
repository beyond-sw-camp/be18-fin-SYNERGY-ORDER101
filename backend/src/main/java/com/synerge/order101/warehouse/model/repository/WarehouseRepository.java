package com.synerge.order101.warehouse.model.repository;


import com.synerge.order101.warehouse.model.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse,Long> {

    @Query("SELECT u FROM Warehouse u ORDER BY function('RAND')")
    List<Warehouse> findRandomWareHouse();
}
