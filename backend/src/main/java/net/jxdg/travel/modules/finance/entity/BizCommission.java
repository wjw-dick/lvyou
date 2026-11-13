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
@TableName("biz_commission")
public class BizCommission {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long orderId;

    private Long distributorId;

    private Long memberId;

    private String type;

    private BigDecimal rate;

    private BigDecimal amount;

    private Integer status;

    private LocalDateTime settleAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
