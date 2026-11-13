package net.jxdg.travel.modules.scenic.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_scenic_staff")
public class BizScenicStaff {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long scenicId;

    private String name;

    private String username;

    private String password;

    private String gate;

    private String phone;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
