package com.szwl.supportbot.knowledgerag.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 向量数据初始化Controller
 * 专门用于初始化向量数据库中的测试数据
 */
@Slf4j
@RestController
@RequestMapping("/api/vector")
public class VectorDataController {

    private final VectorStore vectorStore;

    @Autowired
    public VectorDataController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 初始化营销政策假数据
     * @return 操作结果
     */
    @PostMapping("/init-marketing-data")
    public Map<String, Object> initMarketingData() {
        try {
            log.info("开始初始化营销政策假数据");

            // 构造公司营销政策相关的假数据
            List<Document> allDocuments = List.of(
                // 优惠政策
                new Document("新用户注册即送100元优惠券，首次购买满200元可用，有效期30天。"),
                new Document("VIP会员享受全场9折优惠，生日当月额外赠送50元优惠券。"),
                new Document("学生认证用户专享8.5折优惠，需提供有效学生证或录取通知书。"),
                new Document("企业客户批量采购满10000元享受8折优惠，满50000元享受7.5折优惠。"),
                new Document("老客户推荐新用户注册成功，双方各获得50元优惠券奖励。"),
                
                // 包邮政策
                new Document("全国包邮政策：单笔订单满99元即可享受全国包邮服务。"),
                new Document("偏远地区包邮：新疆、西藏、内蒙古等偏远地区满199元包邮。"),
                new Document("同城配送：北京、上海、广州、深圳等一线城市满59元包邮，当日达。"),
                new Document("海外包邮：港澳台地区满299元包邮，其他海外地区满599元包邮。"),
                new Document("特殊商品包邮：数码产品、家电等大件商品满1999元包邮。"),
                
                // 双十一活动
                new Document("双十一活动时间：11月1日-11月11日，全场商品5折起，部分商品低至3折。"),
                new Document("双十一预热期：10月21日-10月31日，提前加购享受早鸟价优惠。"),
                new Document("双十一当天：11月11日0点-2点，限时2小时超级秒杀，部分商品1折抢购。"),
                new Document("双十一返场：11月12日-11月15日，错过的爆款商品返场特价。"),
                new Document("双十一满减：满300减50，满600减120，满1000减250，上不封顶。"),
                
                // 其他促销活动
                new Document("618年中大促：6月1日-6月18日，全场6折起，满减优惠叠加使用。"),
                new Document("黑色星期五：11月最后一个星期五，海外商品特价促销，限时24小时。"),
                new Document("会员日：每月18日为会员专享日，会员商品额外8折优惠。"),
                new Document("品牌日：每周三为品牌日，指定品牌商品享受特价优惠。"),
                new Document("清仓特卖：季末清仓，过季商品低至2折，数量有限先到先得。"),
                
                // 支付优惠
                new Document("支付宝支付：使用支付宝付款享受随机立减，最高减50元。"),
                new Document("微信支付：微信支付用户专享9.5折优惠，每月限用3次。"),
                new Document("信用卡分期：支持12期免息分期，部分商品支持24期免息。"),
                new Document("花呗支付：花呗用户享受3期免息，6期、12期低息优惠。"),
                new Document("积分抵扣：会员积分可抵扣现金，100积分=1元，最高可抵扣订单金额的50%。"),
                
                // 售后服务政策
                new Document("7天无理由退换货：商品签收后7天内，不影响二次销售可申请退换货。"),
                new Document("15天质量问题退换货：商品出现质量问题，15天内可申请退换货。"),
                new Document("1年质保服务：所有商品提供1年质保，质量问题免费维修或更换。"),
                new Document("延保服务：可购买延保服务，延长质保期至2年或3年。"),
                new Document("上门服务：大件商品提供免费上门安装和调试服务。"),
                
                // 物流配送
                new Document("标准配送：3-5个工作日送达，全国大部分地区覆盖。"),
                new Document("加急配送：1-2个工作日送达，需额外支付加急费用。"),
                new Document("定时配送：可选择指定时间段配送，如上午、下午或晚上。"),
                new Document("自提服务：支持到指定门店自提，免配送费。"),
                new Document("海外直邮：海外商品支持直邮，7-15个工作日送达。")
            );

            // 分批写入，每批最多10个文档
            int batchSize = 10;
            int totalCount = allDocuments.size();
            int batchCount = (totalCount + batchSize - 1) / batchSize; // 向上取整
            
            log.info("开始分批写入数据，总共{}个文档，分{}批写入，每批最多{}个", totalCount, batchCount, batchSize);
            
            for (int i = 0; i < batchCount; i++) {
                int startIndex = i * batchSize;
                int endIndex = Math.min(startIndex + batchSize, totalCount);
                
                List<Document> batch = allDocuments.subList(startIndex, endIndex);
                
                // 保存当前批次到向量存储
                vectorStore.add(batch);
                
                log.info("第{}批数据写入成功，包含{}个文档", i + 1, batch.size());
                
                // 批次间稍作延迟，避免过快写入
                if (i < batchCount - 1) {
                    Thread.sleep(100); // 100ms延迟
                }
            }

            log.info("营销政策假数据初始化成功: 总共{}个文档，分{}批写入", totalCount, batchCount);
            
            return Map.of(
                "success", true,
                "message", "营销政策假数据初始化成功",
                "totalCount", totalCount,
                "batchCount", batchCount,
                "batchSize", batchSize,
                "categories", List.of("优惠政策", "包邮政策", "双十一活动", "其他促销", "支付优惠", "售后服务", "物流配送")
            );

        } catch (Exception e) {
            log.error("初始化营销政策假数据失败", e);
            return Map.of(
                "success", false,
                "message", "初始化营销政策假数据失败: " + e.getMessage()
            );
        }
    }


    /**
     * 健康检查
     * @return 服务状态
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        try {
            // 简单的健康检查
            boolean isHealthy = vectorStore != null;
            
            return Map.of(
                "success", true,
                "healthy", isHealthy,
                "message", isHealthy ? "向量数据库服务正常" : "向量数据库服务异常"
            );

        } catch (Exception e) {
            log.error("健康检查失败", e);
            return Map.of(
                "success", false,
                "healthy", false,
                "message", "健康检查失败: " + e.getMessage()
            );
        }
    }
}