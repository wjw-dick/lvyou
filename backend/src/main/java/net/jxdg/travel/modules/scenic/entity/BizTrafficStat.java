package net.jxdg.travel.modules.scenic.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_traffic_stat")
public class BizTrafficStat {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long scenicId;

    private LocalDate statDate;

    private String timeSlot;

    private String area;

    private Integer inCount;

    private Integer outCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
