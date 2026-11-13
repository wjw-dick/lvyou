package net.jxdg.travel.modules.supplierportal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
@TableName("biz_hotel_room")
public class BizHotelRoom {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long supplierId;

    private String roomType;

    private BigDecimal contractPrice;

    private Integer stock;

    private Integer sold;

    private LocalDate stockDate;

    private Integer status;
}
