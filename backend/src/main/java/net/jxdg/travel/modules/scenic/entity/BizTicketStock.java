package net.jxdg.travel.modules.scenic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;

@Data
@TableName("biz_ticket_stock")
public class BizTicketStock {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long ticketId;

    private LocalDate stockDate;

    private String timeSlot;

    private Integer stock;

    private Integer sold;

    private Integer reserved;
}
