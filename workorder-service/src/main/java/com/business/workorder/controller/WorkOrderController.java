package com.business.workorder.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/workorder")
@CrossOrigin(origins = "*")
public class WorkOrderController {
    
    /**
     * 创建发票申请工单
     */
    @PostMapping("/createTicket")
    public ResponseEntity<Map<String, Object>> createTicket(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("ticketId", 1001L);
        response.put("ticketNumber", "WO-2024-001");
        response.put("status", "PENDING");
        response.put("message", "工单创建成功");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 查询用户工单列表
     */
    @PostMapping("/getUserTickets")
    public ResponseEntity<Map<String, Object>> getUserTickets(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        List<Map<String, Object>> tickets = Arrays.asList(
            Map.of(
                "ticketId", 1001L,
                "ticketNumber", "WO-2024-001",
                "title", "申请开具发票",
                "status", "PENDING",
                "ticketType", "INVOICE_REQUEST",
                "priority", "MEDIUM",
                "createTime", "2024-09-01T10:00:00",
                "updateTime", "2024-09-01T10:00:00"
            ),
            Map.of(
                "ticketId", 1002L,
                "ticketNumber", "WO-2024-002",
                "title", "产品咨询",
                "status", "IN_PROGRESS",
                "ticketType", "PRODUCT_INQUIRY",
                "priority", "LOW",
                "createTime", "2024-09-01T11:00:00",
                "updateTime", "2024-09-01T12:00:00"
            ),
            Map.of(
                "ticketId", 1003L,
                "ticketNumber", "WO-2024-003",
                "title", "退货申请",
                "status", "RESOLVED",
                "ticketType", "RETURN_REQUEST",
                "priority", "HIGH",
                "createTime", "2024-08-30T09:00:00",
                "updateTime", "2024-08-31T15:00:00"
            ),
            Map.of(
                "ticketId", 1004L,
                "ticketNumber", "WO-2024-004",
                "title", "物流查询",
                "status", "PENDING",
                "ticketType", "LOGISTICS_INQUIRY",
                "priority", "MEDIUM",
                "createTime", "2024-09-02T08:00:00",
                "updateTime", "2024-09-02T08:00:00"
            ),
            Map.of(
                "ticketId", 1005L,
                "ticketNumber", "WO-2024-005",
                "title", "技术支持",
                "status", "CLOSED",
                "ticketType", "TECH_SUPPORT",
                "priority", "LOW",
                "createTime", "2024-08-28T14:00:00",
                "updateTime", "2024-08-30T16:00:00"
            )
        );
        
        response.put("tickets", tickets);
        response.put("total", 5);
        response.put("page", 1);
        response.put("size", 10);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取工单详情
     */
    @PostMapping("/getTicketDetail")
    public ResponseEntity<Map<String, Object>> getTicketDetail(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        List<Map<String, Object>> comments = Arrays.asList(
            Map.of(
                "commentId", 2001L,
                "content", "工单已创建，正在处理中",
                "commentType", "SYSTEM",
                "author", "系统",
                "createTime", "2024-09-01T10:00:00"
            ),
            Map.of(
                "commentId", 2002L,
                "content", "已联系财务部门，预计2个工作日内开具发票",
                "commentType", "ADMIN",
                "author", "客服小王",
                "createTime", "2024-09-01T11:00:00"
            )
        );
        
        Map<String, Object> ticket = new HashMap<>();
        ticket.put("ticketId", 1001L);
        ticket.put("ticketNumber", "WO-2024-001");
        ticket.put("title", "申请开具发票");
        ticket.put("description", "订单号：ORD-2024-001，需要开具增值税专用发票");
        ticket.put("status", "PENDING");
        ticket.put("ticketType", "INVOICE_REQUEST");
        ticket.put("priority", "MEDIUM");
        ticket.put("userId", 10001L);
        ticket.put("orderId", 20001L);
        ticket.put("assignee", "客服小王");
        ticket.put("createTime", "2024-09-01T10:00:00");
        ticket.put("updateTime", "2024-09-01T10:00:00");
        ticket.put("comments", comments);
        
        response.put("ticket", ticket);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 更新工单状态
     */
    @PostMapping("/updateTicketStatus")
    public ResponseEntity<Map<String, Object>> updateTicketStatus(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("ticketId", request.get("ticketId"));
        response.put("oldStatus", "PENDING");
        response.put("newStatus", request.get("status"));
        response.put("message", "工单状态更新成功");
        return ResponseEntity.ok(response);
    }
}