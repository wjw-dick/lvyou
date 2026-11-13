package net.jxdg.travel.modules.ota.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_ota_config")
public class BizOtaConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String ota;

    private String appKey;

    private String appSecret;

    private Integer enabled;

    private BigDecimal commissionRate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
