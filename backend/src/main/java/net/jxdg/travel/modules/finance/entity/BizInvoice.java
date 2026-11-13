package net.jxdg.travel.modules.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_invoice")
public class BizInvoice {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String invoiceNo;

    private String title;

    private String taxNo;

    private BigDecimal amount;

    private String type;

    private Long orderId;

    private Long settlementId;

    private Integer status;

    private String applyBy;

    private LocalDateTime issuedAt;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
