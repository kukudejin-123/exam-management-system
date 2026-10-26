package com.exam.service.paper;

import java.util.List;

/**
 * 直接组卷入参：不落组卷规则，按规格当场抽题。
 *
 * <p>给前端组卷器用——教师在界面上拼好规格，先预览再正式组卷，
 * 中途不需要先存一条规则。规则持久化是后续优化，不是组卷的前置条件。
 *
 * @param bankIds     参与抽题的题库
 * @param chapters    章节配比
 * @param types       题型配额
 * @param difficulty  难度配额
 * @param totalScore  目标总分
 * @param paperName   试卷名称
 * @param shuffle     是否打乱输出顺序
 * @param createdBy   创建人
 */
public record ComposeRequest(List<Long> bankIds,
                             List<ChapterQuota> chapters,
                             List<String> types,
                             List<Integer> difficulty,
                             int totalScore,
                             String paperName,
                             boolean shuffle,
                             long createdBy) {

    /** 章节配额 */
    public record ChapterQuota(String chapter, int count) {
    }

    public PaperSpec toSpec() {
        return new PaperSpec(
                chapters == null ? null : chapters.stream()
                        .map(c -> new PaperSpec.ChapterQuota(c.chapter(), c.count()))
                        .toList(),
                difficulty, types, totalScore);
    }
}
