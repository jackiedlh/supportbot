package com.business.trade.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/trade")
@CrossOrigin(origins = "*")
public class TradeController {
    
    /**
     * 查询用户订单列表
     */
    @PostMapping("/getUserOrders")
    public ResponseEntity<Map<String, Object>> getUserOrders(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        List<Map<String, Object>> orders = Arrays.asList(
            Map.of(
                "orderId", 20001L,
                "orderNumber", "ORD-2024-001",
                "status", "DELIVERED",
                "totalAmount", new BigDecimal("9014.00"),
                "currency", "CNY",
                "itemCount", 1,
                "createTime", "2024-08-28T10:00:00",
                "updateTime", "2024-09-01T16:00:00"
            ),
            Map.of(
                "orderId", 20002L,
                "orderNumber", "ORD-2024-002",
                "status", "SHIPPED",
                "totalAmount", new BigDecimal("16019.00"),
                "currency", "CNY",
                "itemCount", 1,
                "createTime", "2024-08-30T14:00:00",
                "updateTime", "2024-09-02T09:00:00"
            ),
            Map.of(
                "orderId", 20003L,
                "orderNumber", "ORD-2024-003",
                "status", "PROCESSING",
                "totalAmount", new BigDecimal("1909.00"),
                "currency", "CNY",
                "itemCount", 1,
                "createTime", "2024-09-01T16:00:00",
                "updateTime", "2024-09-02T10:00:00"
            ),
            Map.of(
                "orderId", 20004L,
                "orderNumber", "ORD-2024-004",
                "status", "DELIVERED",
                "totalAmount", new BigDecimal("4414.00"),
                "currency", "CNY",
                "itemCount", 1,
                "createTime", "2024-08-26T11:00:00",
                "updateTime", "2024-08-31T14:00:00"
            ),
            Map.of(
                "orderId", 20005L,
                "orderNumber", "ORD-2024-005",
                "status", "CANCELLED",
                "totalAmount", new BigDecimal("3009.00"),
                "currency", "CNY",
                "itemCount", 1,
                "createTime", "2024-08-31T13:00:00",
                "updateTime", "2024-09-01T08:00:00"
            )
        );
        
        response.put("orders", orders);
        response.put("total", 5);
        response.put("page", 1);
        response.put("size", 10);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取订单详情
     */
    @PostMapping("/getOrderDetail")
    public ResponseEntity<Map<String, Object>> getOrderDetail(@RequestBody Map<String, Object> request) {
        Long orderId = Long.valueOf(request.get("orderId").toString());
        
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> order = getMockOrderDetail(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        
        response.put("order", order);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 查询物流信息
     */
    @PostMapping("/getShippingTracking")
    public ResponseEntity<Map<String, Object>> getShippingTracking(@RequestBody Map<String, Object> request) {
        Long orderId = Long.valueOf(request.get("orderId").toString());
        
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        
        // 根据订单ID返回不同的物流信息
        if (orderId == 20001L) {
            response.put("trackingNumber", "SF1234567890");
            response.put("carrier", "顺丰速运");
            response.put("status", "DELIVERED");
            response.put("estimatedDelivery", "2024-09-01T16:00:00");
            
            List<Map<String, Object>> events = Arrays.asList(
                Map.of("timestamp", "2024-08-29T10:00:00", "status", "SHIPPED", "description", "包裹已发出", "location", "北京分拣中心"),
                Map.of("timestamp", "2024-08-30T14:00:00", "status", "IN_TRANSIT", "description", "包裹运输中", "location", "天津中转站"),
                Map.of("timestamp", "2024-09-01T08:00:00", "status", "OUT_FOR_DELIVERY", "description", "包裹派送中", "location", "北京朝阳区"),
                Map.of("timestamp", "2024-09-01T16:00:00", "status", "DELIVERED", "description", "包裹已签收", "location", "北京朝阳区")
            );
            response.put("events", events);
        } else if (orderId == 20002L) {
            response.put("trackingNumber", "YTO9876543210");
            response.put("carrier", "圆通速递");
            response.put("status", "IN_TRANSIT");
            response.put("estimatedDelivery", "2024-09-03T18:00:00");
            
            List<Map<String, Object>> events = Arrays.asList(
                Map.of("timestamp", "2024-09-01T16:00:00", "status", "SHIPPED", "description", "包裹已发出", "location", "上海分拣中心"),
                Map.of("timestamp", "2024-09-02T08:00:00", "status", "IN_TRANSIT", "description", "包裹运输中", "location", "上海浦东区")
            );
            response.put("events", events);
        } else if (orderId == 20003L) {
            response.put("trackingNumber", "ZTO5555555555");
            response.put("carrier", "中通快递");
            response.put("status", "PROCESSING");
            response.put("estimatedDelivery", "2024-09-04T20:00:00");
            
            List<Map<String, Object>> events = Arrays.asList(
                Map.of("timestamp", "2024-09-02T10:00:00", "status", "PROCESSING", "description", "订单处理中", "location", "广州仓库")
            );
            response.put("events", events);
        } else if (orderId == 20004L) {
            response.put("trackingNumber", "JD1111111111");
            response.put("carrier", "京东物流");
            response.put("status", "DELIVERED");
            response.put("estimatedDelivery", "2024-08-31T14:00:00");
            
            List<Map<String, Object>> events = Arrays.asList(
                Map.of("timestamp", "2024-08-28T16:00:00", "status", "SHIPPED", "description", "包裹已发出", "location", "深圳分拣中心"),
                Map.of("timestamp", "2024-08-29T10:00:00", "status", "IN_TRANSIT", "description", "包裹运输中", "location", "广州中转站"),
                Map.of("timestamp", "2024-08-30T14:00:00", "status", "IN_TRANSIT", "description", "包裹运输中", "location", "深圳南山区"),
                Map.of("timestamp", "2024-08-31T14:00:00", "status", "DELIVERED", "description", "包裹已签收", "location", "深圳南山区")
            );
            response.put("events", events);
        } else {
            response.put("trackingNumber", "暂无物流信息");
            response.put("carrier", "待分配");
            response.put("status", "PROCESSING");
            response.put("estimatedDelivery", LocalDateTime.now().plusDays(3));
            
            List<Map<String, Object>> events = Arrays.asList(
                Map.of("timestamp", LocalDateTime.now(), "status", "PROCESSING", "description", "订单处理中", "location", "仓库")
            );
            response.put("events", events);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 申请售后(退货/换货)
     */
    @PostMapping("/applyForReturn")
    public ResponseEntity<Map<String, Object>> applyForReturn(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("returnId", 4001L);
        response.put("returnNumber", "RTN-2024-001");
        response.put("status", "PENDING");
        response.put("message", "售后申请已提交，等待审核");
        response.put("createTime", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
    
    private Map<String, Object> getMockOrderDetail(Long orderId) {
        Map<String, Object> order = new HashMap<>();
        
        if (orderId == 20001L) {
            order.put("orderId", 20001L);
            order.put("orderNumber", "ORD-2024-001");
            order.put("status", "DELIVERED");
            order.put("userId", 10001L);
            order.put("subtotal", new BigDecimal("8999.00"));
            order.put("shippingCost", new BigDecimal("15.00"));
            order.put("tax", new BigDecimal("0.00"));
            order.put("totalAmount", new BigDecimal("9014.00"));
            order.put("currency", "CNY");
            order.put("paymentMethod", "微信支付");
            order.put("paymentStatus", "PAID");
            
            Map<String, Object> address = Map.of(
                "recipientName", "张三",
                "phone", "13800138001",
                "address", "北京市朝阳区三里屯街道工体北路8号",
                "city", "北京市",
                "province", "北京市",
                "postalCode", "100027",
                "country", "中国"
            );
            order.put("shippingAddress", address);
            
            List<Map<String, Object>> items = Arrays.asList(
                Map.of(
                    "itemId", 50001L,
                    "productId", 30001L,
                    "productName", "iPhone 15 Pro Max",
                    "quantity", 1,
                    "unitPrice", new BigDecimal("8999.00"),
                    "totalPrice", new BigDecimal("8999.00"),
                    "productImage", "https://example.com/iphone15pro1.jpg"
                )
            );
            order.put("items", items);
            order.put("createTime", "2024-08-28T10:00:00");
            order.put("updateTime", "2024-09-01T16:00:00");
            order.put("shipTime", "2024-08-29T10:00:00");
            order.put("deliveryTime", "2024-09-01T16:00:00");
        } else if (orderId == 20002L) {
            order.put("orderId", 20002L);
            order.put("orderNumber", "ORD-2024-002");
            order.put("status", "SHIPPED");
            order.put("userId", 10002L);
            order.put("subtotal", new BigDecimal("15999.00"));
            order.put("shippingCost", new BigDecimal("20.00"));
            order.put("tax", new BigDecimal("0.00"));
            order.put("totalAmount", new BigDecimal("16019.00"));
            order.put("currency", "CNY");
            order.put("paymentMethod", "支付宝");
            order.put("paymentStatus", "PAID");
            
            Map<String, Object> address = Map.of(
                "recipientName", "李四",
                "phone", "13800138002",
                "address", "上海市浦东新区陆家嘴环路1000号",
                "city", "上海市",
                "province", "上海市",
                "postalCode", "200120",
                "country", "中国"
            );
            order.put("shippingAddress", address);
            
            List<Map<String, Object>> items = Arrays.asList(
                Map.of(
                    "itemId", 50002L,
                    "productId", 30002L,
                    "productName", "MacBook Pro 14英寸",
                    "quantity", 1,
                    "unitPrice", new BigDecimal("15999.00"),
                    "totalPrice", new BigDecimal("15999.00"),
                    "productImage", "https://example.com/macbookpro1.jpg"
                )
            );
            order.put("items", items);
            order.put("createTime", "2024-08-30T14:00:00");
            order.put("updateTime", "2024-09-02T09:00:00");
            order.put("shipTime", "2024-09-01T16:00:00");
            order.put("deliveryTime", null);
        } else if (orderId == 20003L) {
            order.put("orderId", 20003L);
            order.put("orderNumber", "ORD-2024-003");
            order.put("status", "PROCESSING");
            order.put("userId", 10003L);
            order.put("subtotal", new BigDecimal("1899.00"));
            order.put("shippingCost", new BigDecimal("10.00"));
            order.put("tax", new BigDecimal("0.00"));
            order.put("totalAmount", new BigDecimal("1909.00"));
            order.put("currency", "CNY");
            order.put("paymentMethod", "银行卡");
            order.put("paymentStatus", "PAID");
            
            Map<String, Object> address = Map.of(
                "recipientName", "王五",
                "phone", "13800138003",
                "address", "广州市天河区珠江新城花城大道85号",
                "city", "广州市",
                "province", "广东省",
                "postalCode", "510623",
                "country", "中国"
            );
            order.put("shippingAddress", address);
            
            List<Map<String, Object>> items = Arrays.asList(
                Map.of(
                    "itemId", 50003L,
                    "productId", 30003L,
                    "productName", "AirPods Pro (第2代)",
                    "quantity", 1,
                    "unitPrice", new BigDecimal("1899.00"),
                    "totalPrice", new BigDecimal("1899.00"),
                    "productImage", "https://example.com/airpodspro1.jpg"
                )
            );
            order.put("items", items);
            order.put("createTime", "2024-09-01T16:00:00");
            order.put("updateTime", "2024-09-02T10:00:00");
            order.put("shipTime", null);
            order.put("deliveryTime", null);
        } else if (orderId == 20004L) {
            order.put("orderId", 20004L);
            order.put("orderNumber", "ORD-2024-004");
            order.put("status", "DELIVERED");
            order.put("userId", 10004L);
            order.put("subtotal", new BigDecimal("4399.00"));
            order.put("shippingCost", new BigDecimal("15.00"));
            order.put("tax", new BigDecimal("0.00"));
            order.put("totalAmount", new BigDecimal("4414.00"));
            order.put("currency", "CNY");
            order.put("paymentMethod", "微信支付");
            order.put("paymentStatus", "PAID");
            
            Map<String, Object> address = Map.of(
                "recipientName", "赵六",
                "phone", "13800138004",
                "address", "深圳市南山区科技园南区深南大道9988号",
                "city", "深圳市",
                "province", "广东省",
                "postalCode", "518057",
                "country", "中国"
            );
            order.put("shippingAddress", address);
            
            List<Map<String, Object>> items = Arrays.asList(
                Map.of(
                    "itemId", 50004L,
                    "productId", 30004L,
                    "productName", "iPad Air (第5代)",
                    "quantity", 1,
                    "unitPrice", new BigDecimal("4399.00"),
                    "totalPrice", new BigDecimal("4399.00"),
                    "productImage", "https://example.com/ipadair1.jpg"
                )
            );
            order.put("items", items);
            order.put("createTime", "2024-08-26T11:00:00");
            order.put("updateTime", "2024-08-31T14:00:00");
            order.put("shipTime", "2024-08-28T16:00:00");
            order.put("deliveryTime", "2024-08-31T14:00:00");
        } else if (orderId == 20005L) {
            order.put("orderId", 20005L);
            order.put("orderNumber", "ORD-2024-005");
            order.put("status", "CANCELLED");
            order.put("userId", 10005L);
            order.put("subtotal", new BigDecimal("2999.00"));
            order.put("shippingCost", new BigDecimal("10.00"));
            order.put("tax", new BigDecimal("0.00"));
            order.put("totalAmount", new BigDecimal("3009.00"));
            order.put("currency", "CNY");
            order.put("paymentMethod", "支付宝");
            order.put("paymentStatus", "REFUNDED");
            
            Map<String, Object> address = Map.of(
                "recipientName", "孙七",
                "phone", "13800138005",
                "address", "杭州市西湖区文三路259号",
                "city", "杭州市",
                "province", "浙江省",
                "postalCode", "310012",
                "country", "中国"
            );
            order.put("shippingAddress", address);
            
            List<Map<String, Object>> items = Arrays.asList(
                Map.of(
                    "itemId", 50005L,
                    "productId", 30005L,
                    "productName", "Apple Watch Series 9",
                    "quantity", 1,
                    "unitPrice", new BigDecimal("2999.00"),
                    "totalPrice", new BigDecimal("2999.00"),
                    "productImage", "https://example.com/applewatch1.jpg"
                )
            );
            order.put("items", items);
            order.put("createTime", "2024-08-31T13:00:00");
            order.put("updateTime", "2024-09-01T08:00:00");
            order.put("shipTime", null);
            order.put("deliveryTime", null);
        } else {
            return null;
        }
        
        return order;
    }
}