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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        try {
            return anythingLLMClient.chatCompletion(question);
        } catch (Exception e) {
            log.error("调用大模型失败", e);
            return "抱歉，大模型服务暂时不可用，请稍后重试或点击\"转人工处理\"提交工单。";
        }
    }

    @Override
    public void chatQueryStream(String question, java.util.function.Consumer<String> onChunk,
                                Runnable onComplete, java.util.function.Consumer<Exception> onError) {
        anythingLLMClient.chatCompletionStream(question, onChunk, onComplete, onError);
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

    @Override
    public Map<String, Object> syncAllFaqToVector() {
        List<OpsFaq> faqList = list();
        int successCount = 0;
        int failCount = 0;
        for (OpsFaq faq : faqList) {
            boolean success = anythingLLMClient.uploadDocument(faq.getQuestion(), faq.getAnswer());
            if (success) {
                faq.setIsSynced(1);
                updateById(faq);
                successCount++;
            } else {
                failCount++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", faqList.size());
        result.put("success", successCount);
        result.put("fail", failCount);
        return result;
    }
}
