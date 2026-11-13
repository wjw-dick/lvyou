package net.jxdg.travel.modules.product.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_product_price")
public class BizProductPrice {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long productId;

    private String channel;

    private Long channelId;

    private BigDecimal adultPrice;

    private BigDecimal childPrice;

    private BigDecimal singleRoomDiff;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
