package net.jxdg.travel.modules.insurance.entity;

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
@TableName("biz_insurance_order")
public class BizInsuranceOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String policyNo;

    private Long insuranceId;

    private Long orderId;

    private String insuredName;

    private String idNoEnc;

    private String phoneEnc;

    private BigDecimal premium;

    private BigDecimal coverage;

    private Integer status;

    private LocalDate startDate;

    private LocalDate endDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
