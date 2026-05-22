package com.itheima.ops.digital.staff.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.ops.digital.staff.entity.OpsFaq;
import com.itheima.ops.digital.staff.integration.anythingllm.AnythingLLMClient;
import com.itheima.ops.digital.staff.mapper.OpsFaqMapper;
import com.itheima.ops.digital.staff.service.OpsFaqService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OpsFaqServiceImpl extends ServiceImpl<OpsFaqMapper, OpsFaq> implements OpsFaqService {

    @Autowired
    private AnythingLLMClient anythingLLMClient;

    @Override
    public IPage<OpsFaq> pageQuery(Integer pageNum, Integer pageSize, String category, String keyword) {
        Page<OpsFaq> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OpsFaq> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(category), OpsFaq::getCategory, category)
                .and(StringUtils.isNotBlank(keyword), w -> w.like(OpsFaq::getQuestion, keyword).or().like(OpsFaq::getAnswer, keyword));
        return page(page, wrapper);
    }

    @Override
    public String chatQuery(String question) {
        return anythingLLMClient.chatCompletion(question);
    }

    @Override
    public boolean syncFaqToVector(Long faqId) {
        OpsFaq faq = getById(faqId);
        boolean success = anythingLLMClient.uploadDocument(faq.getQuestion(), faq.getAnswer());
        if (success) {
            faq.setIsSynced(1);
            updateById(faq);
        }
        return success;
    }
}
