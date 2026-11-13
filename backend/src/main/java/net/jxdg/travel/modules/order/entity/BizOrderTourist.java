package net.jxdg.travel.modules.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_order_tourist")
public class BizOrderTourist {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long orderId;

    private String name;

    private String idType;

    private String idNoEnc;

    private String idNoMask;

    private String phoneEnc;

    private String phoneMask;

    private Integer isAdult;

    private Integer verifyStatus;

    private LocalDateTime verifyAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
