package com.synerge.order101.store.model.repository;

import com.synerge.order101.store.model.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface StoreRepository extends JpaRepository<Store,Long> {
    Page<Store> findByStoreNameContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("SELECT DISTINCT s.address FROM Store s WHERE s.address IS NOT NULL")
    List<String> findDistinctAddresses();

    // 추가: 주소가 정확히 일치하는 경우 페이징 검색
    Page<Store> findByAddress(String address, Pageable pageable);

    // 추가: 주소와 storeName(부분 일치) 조합으로 페이징 검색
    Page<Store> findByAddressAndStoreNameContainingIgnoreCase(String address, String storeName, Pageable pageable);
}
