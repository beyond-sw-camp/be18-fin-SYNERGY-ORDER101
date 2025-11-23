package com.synerge.order101.product.model.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "product")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_category_id")
    private ProductCategory productCategory;

    @Column(name = "product_code", length = 100, nullable = false, unique = true)
    private String productCode;

    @Column(name = "product_name", length = 200, nullable = false)
    private String productName;

    @Column(name = "image_url")
    private String imageUrl;

    @Column
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean status = true;

    @Column(name = "price", precision = 15, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ProductSupplier> productSupplier = new ArrayList<>();

    public void update(String productCode, String productName, String description, BigDecimal price,
                       String imageUrl, Boolean status, ProductCategory productCategory) {
        this.productCode = productCode;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.status = status;
        this.productCategory = productCategory;
    }
}
