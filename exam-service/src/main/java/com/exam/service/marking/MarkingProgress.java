package com.exam.service.marking;

/** 阅卷进度 */
public record MarkingProgress(int total, int graded, int pending) {

    public double percent() {
        return total == 0 ? 0.0 : (double) graded / total * 100;
    }
}
