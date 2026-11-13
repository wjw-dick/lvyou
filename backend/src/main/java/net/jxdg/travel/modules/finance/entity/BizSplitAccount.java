package net.jxdg.travel.modules.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_split_account")
public class BizSplitAccount {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String name;

    private String type;

    private Long targetId;

    private String wxAccount;

    private BigDecimal splitRate;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
