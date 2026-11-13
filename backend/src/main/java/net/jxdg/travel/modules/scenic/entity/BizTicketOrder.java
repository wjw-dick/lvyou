package net.jxdg.travel.modules.scenic.entity;

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
@TableName("biz_ticket_order")
public class BizTicketOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String ticketNo;

    private Long ticketId;

    private Long memberId;

    private String visitorName;

    private String idNoEnc;

    private LocalDate visitDate;

    private String timeSlot;

    private Integer quantity;

    private BigDecimal amount;

    private Integer status;

    private Integer verifyCount;

    private String qrCode;

    private String gate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
