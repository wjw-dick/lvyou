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
@TableName("biz_scenic_reservation")
public class BizScenicReservation {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long ticketOrderId;

    private Long ticketId;

    private LocalDate visitDate;

    private String timeSlot;

    private Integer quantity;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
