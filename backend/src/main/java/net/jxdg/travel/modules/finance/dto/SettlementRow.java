package net.jxdg.travel.modules.finance.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SettlementRow {
    @ExcelProperty("结算单号")
    private String settleNo;
    @ExcelProperty("类型")
    private String type;
    @ExcelProperty("合作方")
    private String partnerName;
    @ExcelProperty("金额")
    private BigDecimal amount;
    @ExcelProperty("已付金额")
    private BigDecimal paidAmount;
    @ExcelProperty("退款")
    private BigDecimal refundAmount;
    @ExcelProperty("佣金")
    private BigDecimal commission;
    @ExcelProperty("真实到账")
    private BigDecimal realAmount;
    @ExcelProperty("状态")
    private String status;
}
