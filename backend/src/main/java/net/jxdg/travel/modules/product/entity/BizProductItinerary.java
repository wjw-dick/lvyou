package net.jxdg.travel.modules.product.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_product_itinerary")
public class BizProductItinerary {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long productId;

    private Integer dayNo;

    private String title;

    private String spots;

    private String hotel;

    private String meals;

    private String transport;

    private String guide;

    private String accommodation;

    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
