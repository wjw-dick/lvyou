package net.jxdg.travel.modules.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_audit_log")
public class SysAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long operatorId;

    private String operatorName;

    private String userType;

    private String module;

    private String action;

    private String target;

    private String detail;

    private String ip;

    private String result;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
