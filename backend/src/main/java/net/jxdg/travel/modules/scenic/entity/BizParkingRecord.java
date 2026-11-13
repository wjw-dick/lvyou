package net.jxdg.travel.modules.scenic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_parking_record")
public class BizParkingRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long scenicId;

    private String plateNo;

    private LocalDateTime entryTime;

    private LocalDateTime exitTime;

    private BigDecimal fee;

    private Integer status;
}
