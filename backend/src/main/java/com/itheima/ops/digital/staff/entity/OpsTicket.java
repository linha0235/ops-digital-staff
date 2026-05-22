package com.itheima.ops.digital.staff.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ops_ticket")
public class OpsTicket {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reporterName;
    private String reporterPhone;
    private String problemTitle;
    private String problemDesc;
    private String problemImages;
    private Integer status;
    private Long handlerId;
    private String handleResult;
    private Integer visitStatus;
    private Integer satisfaction;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
