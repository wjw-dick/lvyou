package net.jxdg.travel.modules.scenic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("biz_scenic_area")
public class BizScenicArea {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long scenicId;

    private String name;

    private Integer capacity;

    private Integer inCount;

    private BigDecimal longitude;

    private BigDecimal latitude;
}
