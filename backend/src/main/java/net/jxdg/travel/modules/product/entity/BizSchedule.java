package net.jxdg.travel.modules.product.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_schedule")
public class BizSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String scheduleNo;

    private Long productId;

    private LocalDate departDate;

    private LocalDate returnDate;

    private String meetingPoint;

    private String meetingTime;

    private Long guideId;

    private Integer status;

    private Integer quota;

    private Integer sold;

    private Integer minGroup;

    private Integer warnDays;

    private String planPdf;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
