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
@TableName("biz_guarantee_fund")
public class BizGuaranteeFund {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String name;

    private String type;

    private BigDecimal amount;

    private BigDecimal paidAmount;

    private LocalDate expireDate;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
