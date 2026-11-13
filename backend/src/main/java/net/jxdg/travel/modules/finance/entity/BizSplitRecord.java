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
@TableName("biz_split_record")
public class BizSplitRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long orderId;

    private Long accountId;

    private BigDecimal amount;

    private BigDecimal rate;

    private Integer status;

    private Integer retryCount;

    private String errorMsg;

    private String splitNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
