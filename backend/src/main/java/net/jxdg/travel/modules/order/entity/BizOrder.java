package net.jxdg.travel.modules.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_order")
public class BizOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String orderNo;

    private Long memberId;

    private Long productId;

    private Long scheduleId;

    private String productTitle;

    private LocalDate departDate;

    private String contactName;

    private String contactPhone;

    private Integer adultCount;

    private Integer childCount;

    private BigDecimal totalAmount;

    private BigDecimal paidAmount;

    private BigDecimal refundAmount;

    private BigDecimal commissionAmount;

    private Integer payStatus;

    private Integer verifyStatus;

    private Integer status;

    private String channel;

    private String source;

    private Long distributorId;

    private String otaOrderNo;

    private String remark;

    private String cancelReason;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
