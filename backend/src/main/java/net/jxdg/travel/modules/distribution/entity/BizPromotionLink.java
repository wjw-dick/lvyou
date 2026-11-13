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
@TableName("biz_promotion_link")
public class BizPromotionLink {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long memberId;

    private Long distributorId;

    private Long productId;

    private String code;

    private String type;

    private Integer clicks;

    private Integer orders;

    private BigDecimal commission;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
