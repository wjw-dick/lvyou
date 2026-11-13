package net.jxdg.travel.modules.guide.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_guide_assignment")
public class BizGuideAssignment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long scheduleId;

    private Long guideId;

    private Integer status;

    private LocalDateTime pushTime;

    private LocalDateTime confirmTime;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
