package net.jxdg.travel.modules.product.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_product")
public class BizProduct {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String productNo;

    private String category;

    private String title;

    private String subtitle;

    private Integer days;

    private String departure;

    private String destination;

    private Long destinationId;

    private BigDecimal adultPrice;

    private BigDecimal childPrice;

    private BigDecimal singleRoomDiff;

    private BigDecimal costPrice;

    private String includeText;

    private String excludeText;

    private String refundRule;

    private String meetingPoint;

    private String cover;

    private String images;

    private String guideContent;

    private Integer stock;

    private Integer sold;

    private Integer minGroup;

    private String channels;

    private Integer status;

    private Integer sort;

    private String remark;

    @TableLogic
    private Integer deleted;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
