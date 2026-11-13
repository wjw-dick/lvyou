package net.jxdg.travel.modules.ota.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_ota_order")
public class BizOtaOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String ota;

    private String otaOrderNo;

    private Long orderId;

    private BigDecimal amount;

    private BigDecimal settleAmount;

    private BigDecimal commission;

    private BigDecimal fee;

    private BigDecimal refundAmount;

    private BigDecimal supplementAmount;

    private String status;

    private String rawJson;

    private LocalDateTime syncedAt;
}
