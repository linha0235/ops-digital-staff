package com.itheima.ops.digital.staff.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ops_faq")
public class OpsFaq {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String question;
    private String answer;
    private String category;
    private Integer isSynced;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
