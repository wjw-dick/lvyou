package net.jxdg.travel.modules.report.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderReportRow {
    @ExcelProperty("订单号")
    private String orderNo;
    @ExcelProperty("线路")
    private String productTitle;
    @ExcelProperty("出团日期")
    private String departDate;
    @ExcelProperty("人数")
    private Integer people;
    @ExcelProperty("金额")
    private BigDecimal amount;
    @ExcelProperty("退款")
    private BigDecimal refund;
    @ExcelProperty("渠道")
    private String channel;
    @ExcelProperty("状态")
    private String status;
    @ExcelProperty("下单时间")
    private String createdAt;
}
