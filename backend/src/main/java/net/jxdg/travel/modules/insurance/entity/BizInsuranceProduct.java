package net.jxdg.travel.modules.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("biz_insurance_product")
public class BizInsuranceProduct {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String name;

    private String company;

    private String scope;

    private String coverageJson;

    private BigDecimal premium;

    private Integer status;
}
