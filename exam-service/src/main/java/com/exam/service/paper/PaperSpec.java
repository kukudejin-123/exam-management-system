package com.exam.service.paper;

import java.util.List;

/**
 * 组卷规格：从规则 JSON 解析出来的结构化抽题要求。
 *
 * <p>对应"选题方式枚举"五个维度中的三个可量化维度（章节、难度、题型），
 * 随机与顺序由组卷时决定，不写在规格里。
 *
 * @param chapters  按章节配比，每章抽几题
 * @param difficulty 难度分布，如 [1,2,2] 表示 1 道易题、2 道中档、2 道难题
 * @param types     题型配额，如 ["SINGLE","JUDGE"]
 * @param totalScore 目标总分
 */
public record PaperSpec(List<ChapterQuota> chapters,
                        List<Integer> difficulty,
                        List<String> types,
                        int totalScore) {

    /** 单章配额 */
    public record ChapterQuota(String chapter, int count) {
    }

    /**
     * 题目总题量：三个维度配额之和。
     *
     * <p>必须三个都算——只按章节算会让"只配题型/难度"的规格总题量为 0 而被误拒。
     */
    public int totalQuestions() {
        int byChapter = chapters == null ? 0 : chapters.stream().mapToInt(ChapterQuota::count).sum();
        int byType = types == null ? 0 : types.size();
        int byDifficulty = difficulty == null ? 0 : difficulty.size();
        return byChapter + byType + byDifficulty;
    }
}
