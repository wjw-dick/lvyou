package net.jxdg.travel.modules.distribution.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_distributor_price")
public class BizDistributorPrice {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long distributorId;

    private Long productId;

    private BigDecimal adultPrice;

    private BigDecimal childPrice;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
