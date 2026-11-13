package net.jxdg.travel.modules.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("biz_settlement_item")
public class BizSettlementItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long settlementId;

    private String bizType;

    private Long bizId;

    private String bizNo;

    private BigDecimal amount;

    private String remark;
}
