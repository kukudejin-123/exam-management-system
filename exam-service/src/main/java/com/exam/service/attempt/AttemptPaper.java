package com.exam.service.attempt;

import java.util.List;

/** 考生视角的卷面 */
public record AttemptPaper(long attemptId, int attemptNo, String examName,
                           Integer durationMinutes, long remainingSeconds,
                           List<AttemptQuestion> questions) {
}
