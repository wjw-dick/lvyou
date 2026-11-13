package net.jxdg.travel.modules.content.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_ai_conversation")
public class BizAiConversation {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long memberId;

    private String question;

    private String answer;

    private String intent;

    private Integer transferred;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
