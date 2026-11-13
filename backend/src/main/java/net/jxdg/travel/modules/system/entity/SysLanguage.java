package net.jxdg.travel.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_language")
public class SysLanguage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private Integer enabled;

    private Integer sort;
}
