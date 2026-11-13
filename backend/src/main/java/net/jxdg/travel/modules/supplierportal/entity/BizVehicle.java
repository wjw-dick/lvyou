package net.jxdg.travel.modules.supplierportal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("biz_vehicle")
public class BizVehicle {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long supplierId;

    private String plateNo;

    private String model;

    private Integer seats;

    private String driverName;

    private String driverPhone;

    private BigDecimal pricePerDay;

    private BigDecimal fuelPer100km;

    private BigDecimal mileage;

    private Integer status;
}
