package net.jxdg.travel.modules.guide.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_guide_expense")
public class BizGuideExpense {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long scheduleId;

    private Long guideId;

    private String type;

    private BigDecimal amount;

    private String receiptImages;

    private String remark;

    private Integer status;

    private String auditBy;

    private LocalDateTime auditAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
