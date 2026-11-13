package net.jxdg.travel.modules.member.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_activity")
public class BizActivity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String name;

    private String type;

    private String ruleJson;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private String statJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
