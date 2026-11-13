package net.jxdg.travel.modules.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_verify_record")
public class BizVerifyRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long orderId;

    private Long touristId;

    private Long ticketOrderId;

    private String code;

    private String verifyType;

    private Long operatorId;

    private String operatorName;

    private String gate;

    private String result;

    private String remark;

    private LocalDateTime verifyAt;
}
