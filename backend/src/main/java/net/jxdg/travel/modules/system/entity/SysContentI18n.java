package net.jxdg.travel.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_content_i18n")
public class SysContentI18n {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String bizType;

    private Long bizId;

    private String field;

    private String lang;

    private String content;
}
