package net.jxdg.travel.modules.supplierportal.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_vehicle_dispatch")
public class BizVehicleDispatch {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long vehicleId;

    private Long scheduleId;

    private String driverName;

    private LocalDateTime departTime;

    private Integer status;

    private LocalDateTime verifyAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
