package net.jxdg.travel.modules.guide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("biz_guide_audio")
public class BizGuideAudio {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long scenicId;

    private String spotName;

    private String audioUrl;

    private String mapUrl;

    private String qrCode;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String content;
}
