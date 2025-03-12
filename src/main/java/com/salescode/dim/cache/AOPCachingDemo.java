package com.salescode.dim.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Log
public class AOPCachingDemo {

    public static void main(String[] args) {
        // Setup service
        ProductService service = new ProductService();

        log.info("==== First call - should miss cache ====");
        Product product1 = service.getProductById(1L);
        log.info("Retrieved: " + product1);

        log.info("\n\n==== Second call - should hit cache ====");
        Product product2 = service.getProductById(1L);
        log.info("Retrieved: " + product2);

        log.info("\n\n==== First category call - should miss cache ====");
         List<Product> electronics = service.getProductsByCategory("Electronics");
        log.info("Retrieved " + electronics.size() + " electronic products");

        log.info("\n\n==== Second category call - should hit cache ====");
        electronics = service.getProductsByCategory("Electronics");
        log.info("Retrieved " + electronics.size() + " electronic products");

        log.info("\n\n==== Updating product - should evict from cache ====");
        product1.setPrice(1199.99);
        service.updateProduct(product1);

        log.info("\n\n==== Fetching product after update - should miss cache ====");
        Product updatedProduct = service.getProductById(1L);
        log.info("Updated product: " + updatedProduct);

        log.info("\n\n==== Clearing category cache ====");
        service.clearCategoryCache();

        log.info("\n\n==== Fetching category after clear - should miss cache ====");
        electronics = service.getProductsByCategory("Electronics");
        log.info("Retrieved " + electronics.size() + " electronic products");

        log.info("\n\n==== Cache Statistics ====");
        CacheManager.getInstance().printStats();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        private Long id;
        private String name;
        private String category;
        private double price;
    }

    @Log
    public static class ProductService {
        private final Map<Long, Product> products = new HashMap<>();

        public ProductService() {
            // Initialize with sample data
            products.put(1L, new Product(1L, "Laptop", "Electronics", 1299.99));
            products.put(2L, new Product(2L, "Smartphone", "Electronics", 799.99));
            products.put(3L, new Product(3L, "Desk Chair", "Furniture", 249.99));
            products.put(4L, new Product(4L, "Coffee Table", "Furniture", 349.99));
            products.put(5L, new Product(5L, "Blender", "Kitchen", 89.99));
        }

        @Cacheable(cacheName = "products", expireAfterMinutes = 30)
        public Product getProductById(Long id) {
            log.info("DATABASE CALL: Fetching product from database: " + id);

            // Simulate database latency
            sleep(500);

            return products.get(id);
        }

        @Cacheable(cacheName = "products-by-category", maximumSize = 200)
        public List<Product> getProductsByCategory(String category) {
            log.info("DATABASE CALL: Fetching products for category: " + category);

            // Simulate database latency
            sleep(800);

            return products.values().stream()
                    .filter(p -> p.getCategory().equals(category))
                    .collect(Collectors.toList());
        }

        @CacheEvict(cacheName = "products")
        public void updateProduct(Product product) {
            log.info("DATABASE CALL: Updating product: " + product.getId());

            // Simulate database latency
            sleep(300);

            products.put(product.getId(), product);
        }

        @CacheEvict(cacheName = "products-by-category", allEntries = true)
        public void clearCategoryCache() {
            log.info("Clearing all category caches");
        }

        private void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                log.log(Level.WARNING, "Sleep interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}