package net.jxdg.travel.modules.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_message_template")
public class SysMessageTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String bizType;

    private String templateId;

    private String title;

    private String fieldsJson;

    private String channel;

    private Integer enabled;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
