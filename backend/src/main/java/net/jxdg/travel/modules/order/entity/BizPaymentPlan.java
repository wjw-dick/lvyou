package net.jxdg.travel.modules.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
@TableName("biz_payment_plan")
public class BizPaymentPlan {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long settlementId;

    private String direction;

    private LocalDate planDate;

    private BigDecimal amount;

    private Integer status;

    private String remark;
}
