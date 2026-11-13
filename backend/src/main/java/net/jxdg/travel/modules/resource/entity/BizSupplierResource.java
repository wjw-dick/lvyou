package net.jxdg.travel.modules.resource.entity;

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
@TableName("biz_supplier_resource")
public class BizSupplierResource {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long supplierId;

    private String name;

    private String type;

    private String spec;

    private String unit;

    private Integer stock;

    private Integer sold;

    private BigDecimal costPrice;

    private BigDecimal salePrice;

    private LocalDate availableDate;

    private Integer status;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
