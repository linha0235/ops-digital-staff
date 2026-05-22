package com.itheima.ops.digital.staff.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.ops.digital.staff.entity.OpsTicket;

public interface OpsTicketService extends IService<OpsTicket> {
    IPage<OpsTicket> pageQuery(Integer pageNum, Integer pageSize, Integer status);
    void createTicket(OpsTicket ticket);
    void handleTicket(Long ticketId, Long handlerId, String handleResult);
    void addSatisfaction(Long ticketId, Integer satisfaction);
}
