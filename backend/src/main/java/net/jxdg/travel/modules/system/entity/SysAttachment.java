package net.jxdg.travel.modules.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_attachment")
public class SysAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String bizType;

    private Long bizId;

    private String name;

    private String url;

    private Long size;

    private String ext;

    private String storageDriver;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
