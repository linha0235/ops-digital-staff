package com.itheima.ops.digital.staff.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.ops.digital.staff.entity.OpsTicket;
import com.itheima.ops.digital.staff.mapper.OpsTicketMapper;
import com.itheima.ops.digital.staff.service.OpsTicketService;
import org.springframework.stereotype.Service;

@Service
public class OpsTicketServiceImpl extends ServiceImpl<OpsTicketMapper, OpsTicket> implements OpsTicketService {

    @Override
    public IPage<OpsTicket> pageQuery(Integer pageNum, Integer pageSize, Integer status) {
        Page<OpsTicket> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OpsTicket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, OpsTicket::getStatus, status).orderByDesc(OpsTicket::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public void createTicket(OpsTicket ticket) {
        ticket.setStatus(0);
        ticket.setVisitStatus(0);
        save(ticket);
    }

    @Override
    public void handleTicket(Long ticketId, Long handlerId, String handleResult) {
        OpsTicket ticket = new OpsTicket();
        ticket.setId(ticketId);
        ticket.setHandlerId(handlerId);
        ticket.setHandleResult(handleResult);
        ticket.setStatus(2);
        updateById(ticket);
    }

    @Override
    public void addSatisfaction(Long ticketId, Integer satisfaction) {
        OpsTicket ticket = new OpsTicket();
        ticket.setId(ticketId);
        ticket.setVisitStatus(1);
        ticket.setSatisfaction(satisfaction);
        updateById(ticket);
    }
}
