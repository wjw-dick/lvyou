package net.jxdg.travel.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_purchase_order")
public class BizPurchaseOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String purchaseNo;

    private Long scheduleId;

    private Long supplierId;

    private Long resourceId;

    private String resourceName;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private Integer status;

    private LocalDateTime confirmTime;

    private String confirmRemark;

    private Integer settleStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
