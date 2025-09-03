package com.business.product.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/product")
@CrossOrigin(origins = "*")
public class ProductController {
    
    /**
     * 获取指定商品的详细信息
     */
    @PostMapping("/getProductDetail")
    public ResponseEntity<Map<String, Object>> getProductDetail(@RequestBody Map<String, Object> request) {
        Long productId = Long.valueOf(request.get("productId").toString());
        
        Map<String, Object> response = new HashMap<>();
        
        // 模拟5个商品数据
        Map<String, Object> product = getMockProduct(productId);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        
        response.put("product", product);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 查询指定商品的库存和预计发货时间
     */
    @PostMapping("/getProductAvailability")
    public ResponseEntity<Map<String, Object>> getProductAvailability(@RequestBody Map<String, Object> request) {
        Long productId = Long.valueOf(request.get("productId").toString());
        String region = request.get("region") != null ? request.get("region").toString() : "北京";
        Integer quantity = request.get("quantity") != null ? Integer.valueOf(request.get("quantity").toString()) : 1;
        
        Map<String, Object> response = new HashMap<>();
        response.put("productId", productId);
        
        // 根据商品ID返回不同的库存数据
        if (productId == 30001L) {
            response.put("availableStock", 136);
            response.put("reservedStock", 20);
            response.put("totalStock", 156);
            response.put("stockStatus", "IN_STOCK");
        } else if (productId == 30002L) {
            response.put("availableStock", 69);
            response.put("reservedStock", 20);
            response.put("totalStock", 89);
            response.put("stockStatus", "IN_STOCK");
        } else if (productId == 30003L) {
            response.put("availableStock", 214);
            response.put("reservedStock", 20);
            response.put("totalStock", 234);
            response.put("stockStatus", "IN_STOCK");
        } else if (productId == 30004L) {
            response.put("availableStock", 47);
            response.put("reservedStock", 20);
            response.put("totalStock", 67);
            response.put("stockStatus", "LOW_STOCK");
        } else if (productId == 30005L) {
            response.put("availableStock", 103);
            response.put("reservedStock", 20);
            response.put("totalStock", 123);
            response.put("stockStatus", "IN_STOCK");
        } else {
            response.put("availableStock", 0);
            response.put("reservedStock", 0);
            response.put("totalStock", 0);
            response.put("stockStatus", "OUT_OF_STOCK");
        }
        
        // 根据地区计算发货时间
        int estimatedDays = 1;
        if ("北京".equals(region) || "上海".equals(region) || "广州".equals(region)) {
            estimatedDays = 1;
        } else if ("深圳".equals(region) || "杭州".equals(region) || "南京".equals(region)) {
            estimatedDays = 2;
        } else {
            estimatedDays = 3;
        }
        
        response.put("estimatedDays", estimatedDays);
        response.put("estimatedShipDate", LocalDateTime.now().plusDays(estimatedDays));
        response.put("shippingMethod", "标准快递");
        response.put("shippingCost", new BigDecimal("15.00"));
        
        return ResponseEntity.ok(response);
    }
    
    private Map<String, Object> getMockProduct(Long productId) {
        Map<String, Object> product = new HashMap<>();
        
        if (productId == 30001L) {
            product.put("productId", 30001L);
            product.put("name", "iPhone 15 Pro Max");
            product.put("description", "苹果最新旗舰手机，搭载A17 Pro芯片，钛金属设计");
            product.put("price", new BigDecimal("8999.00"));
            product.put("currency", "CNY");
            product.put("stock", 156);
            product.put("category", "手机");
            product.put("images", Arrays.asList(
                "https://example.com/iphone15pro1.jpg",
                "https://example.com/iphone15pro2.jpg",
                "https://example.com/iphone15pro3.jpg"
            ));
            Map<String, String> specs = new HashMap<>();
            specs.put("屏幕", "6.7英寸超视网膜XDR显示屏");
            specs.put("处理器", "A17 Pro芯片");
            specs.put("存储", "256GB");
            specs.put("摄像头", "4800万像素主摄");
            specs.put("颜色", "深空黑色");
            product.put("specifications", specs);
            product.put("brand", "Apple");
            product.put("model", "iPhone 15 Pro Max");
            product.put("rating", new BigDecimal("4.8"));
            product.put("reviewCount", 2847);
            product.put("status", "ACTIVE");
        } else if (productId == 30002L) {
            product.put("productId", 30002L);
            product.put("name", "MacBook Pro 14英寸");
            product.put("description", "专业级笔记本电脑，搭载M3 Pro芯片，适合专业用户");
            product.put("price", new BigDecimal("15999.00"));
            product.put("currency", "CNY");
            product.put("stock", 89);
            product.put("category", "笔记本电脑");
            product.put("images", Arrays.asList(
                "https://example.com/macbookpro1.jpg",
                "https://example.com/macbookpro2.jpg"
            ));
            Map<String, String> specs = new HashMap<>();
            specs.put("屏幕", "14.2英寸Liquid Retina XDR显示屏");
            specs.put("处理器", "M3 Pro芯片");
            specs.put("内存", "18GB统一内存");
            specs.put("存储", "512GB SSD");
            specs.put("颜色", "深空灰色");
            product.put("specifications", specs);
            product.put("brand", "Apple");
            product.put("model", "MacBook Pro 14");
            product.put("rating", new BigDecimal("4.9"));
            product.put("reviewCount", 1234);
            product.put("status", "ACTIVE");
        } else if (productId == 30003L) {
            product.put("productId", 30003L);
            product.put("name", "AirPods Pro (第2代)");
            product.put("description", "主动降噪无线耳机，支持空间音频");
            product.put("price", new BigDecimal("1899.00"));
            product.put("currency", "CNY");
            product.put("stock", 234);
            product.put("category", "耳机");
            product.put("images", Arrays.asList(
                "https://example.com/airpodspro1.jpg",
                "https://example.com/airpodspro2.jpg"
            ));
            Map<String, String> specs = new HashMap<>();
            specs.put("降噪", "主动降噪技术");
            specs.put("续航", "最长6小时聆听时间");
            specs.put("充电盒", "MagSafe充电盒");
            specs.put("防水", "IPX4级抗汗抗水");
            specs.put("颜色", "白色");
            product.put("specifications", specs);
            product.put("brand", "Apple");
            product.put("model", "AirPods Pro 2");
            product.put("rating", new BigDecimal("4.7"));
            product.put("reviewCount", 3456);
            product.put("status", "ACTIVE");
        } else if (productId == 30004L) {
            product.put("productId", 30004L);
            product.put("name", "iPad Air (第5代)");
            product.put("description", "轻薄平板电脑，搭载M1芯片，支持Apple Pencil");
            product.put("price", new BigDecimal("4399.00"));
            product.put("currency", "CNY");
            product.put("stock", 67);
            product.put("category", "平板电脑");
            product.put("images", Arrays.asList(
                "https://example.com/ipadair1.jpg",
                "https://example.com/ipadair2.jpg"
            ));
            Map<String, String> specs = new HashMap<>();
            specs.put("屏幕", "10.9英寸Liquid Retina显示屏");
            specs.put("处理器", "M1芯片");
            specs.put("存储", "64GB");
            specs.put("摄像头", "1200万像素广角摄像头");
            specs.put("颜色", "星光色");
            product.put("specifications", specs);
            product.put("brand", "Apple");
            product.put("model", "iPad Air 5");
            product.put("rating", new BigDecimal("4.6"));
            product.put("reviewCount", 1890);
            product.put("status", "ACTIVE");
        } else if (productId == 30005L) {
            product.put("productId", 30005L);
            product.put("name", "Apple Watch Series 9");
            product.put("description", "智能手表，健康监测，运动追踪");
            product.put("price", new BigDecimal("2999.00"));
            product.put("currency", "CNY");
            product.put("stock", 123);
            product.put("category", "智能手表");
            product.put("images", Arrays.asList(
                "https://example.com/applewatch1.jpg",
                "https://example.com/applewatch2.jpg"
            ));
            Map<String, String> specs = new HashMap<>();
            specs.put("屏幕", "45毫米Always-On视网膜显示屏");
            specs.put("处理器", "S9 SiP芯片");
            specs.put("存储", "64GB");
            specs.put("防水", "50米防水");
            specs.put("颜色", "午夜色");
            product.put("specifications", specs);
            product.put("brand", "Apple");
            product.put("model", "Apple Watch Series 9");
            product.put("rating", new BigDecimal("4.5"));
            product.put("reviewCount", 2156);
            product.put("status", "ACTIVE");
        } else {
            return null;
        }
        
        return product;
    }
}