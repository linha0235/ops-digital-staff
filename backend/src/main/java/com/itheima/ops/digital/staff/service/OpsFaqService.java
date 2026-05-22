package com.itheima.ops.digital.staff.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.ops.digital.staff.entity.OpsFaq;

public interface OpsFaqService extends IService<OpsFaq> {
    IPage<OpsFaq> pageQuery(Integer pageNum, Integer pageSize, String category, String keyword);
    String chatQuery(String question);
    boolean syncFaqToVector(Long faqId);
}
