package com.exam.service.marking;

/**
 * 阅卷队列条目。
 *
 * <p>revealNames=false 时 studentName 恒为 null——匿名评阅的约束落在
 * Service 层构造对象的地方，而不是靠前端不显示。
 */
public record MarkingItem(long attemptId, Long studentId, String studentName,
                          String answer, Integer gained) {

    public boolean graded() {
        return gained != null;
    }
}
