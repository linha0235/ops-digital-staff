package com.itheima.ops.digital.staff.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.ops.digital.staff.entity.OpsFaq;

import java.util.Map;
import java.util.function.Consumer;

public interface OpsFaqService extends IService<OpsFaq> {
    IPage<OpsFaq> pageQuery(Integer pageNum, Integer pageSize, String category, String keyword);
    String chatQuery(String question);
    void chatQueryStream(String question, Consumer<String> onChunk,
                         Runnable onComplete, Consumer<Exception> onError);
    boolean syncFaqToVector(Long faqId);
    Map<String, Object> syncAllFaqToVector();
}
