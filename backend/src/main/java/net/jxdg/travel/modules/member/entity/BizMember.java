package net.jxdg.travel.modules.member.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_member")
public class BizMember {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String unionId;

    private String openId;

    private String nickname;

    private String avatar;

    private String phoneEnc;

    private String phoneMask;

    private Integer level;

    private Integer points;

    private BigDecimal balance;

    private BigDecimal totalConsume;

    private Integer status;

    private LocalDateTime lastActiveAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
