package com.exam.service.question;

import com.exam.repository.entity.QuestionBank;
import com.exam.repository.mapper.QuestionBankMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/** 题库查询 */
@Service
public class QuestionBankService {

    private final QuestionBankMapper bankMapper;

    public QuestionBankService(QuestionBankMapper bankMapper) {
        this.bankMapper = bankMapper;
    }

    /** 全部启用中的题库，供组卷器的下拉选项 */
    public List<QuestionBank> listAll() {
        return bankMapper.selectList(null);
    }
}
