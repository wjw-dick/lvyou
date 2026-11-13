package net.jxdg.travel.modules.contract.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_contract")
public class BizContract {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String contractNo;

    private Long orderId;

    private Long memberId;

    private Long templateId;

    private String title;

    private String content;

    private Integer signStatus;

    private String signUrl;

    private LocalDateTime signTime;

    private Integer faceVerified;

    private String caCert;

    private String pdfUrl;

    private Integer syncedGov;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
