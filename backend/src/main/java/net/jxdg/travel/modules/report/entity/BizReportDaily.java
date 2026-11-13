package net.jxdg.travel.modules.report.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_report_daily")
public class BizReportDaily {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private LocalDate statDate;

    private Integer orderCount;

    private BigDecimal salesAmount;

    private BigDecimal settleAmount;

    private Integer memberCount;

    private BigDecimal groupRate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
