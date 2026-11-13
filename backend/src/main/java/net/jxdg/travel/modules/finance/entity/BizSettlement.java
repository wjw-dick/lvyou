package net.jxdg.travel.modules.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_settlement")
public class BizSettlement {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String settleNo;

    private String type;

    private String partnerType;

    private Long partnerId;

    private String partnerName;

    private Long scheduleId;

    private Long orderId;

    private BigDecimal amount;

    private BigDecimal paidAmount;

    private BigDecimal refundAmount;

    private BigDecimal commission;

    private BigDecimal realAmount;

    private Integer status;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private Integer approvalStatus;

    private String remark;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
