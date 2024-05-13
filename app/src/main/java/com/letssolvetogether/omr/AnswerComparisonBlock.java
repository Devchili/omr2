package com.letssolvetogether.omr;

public class AnswerComparisonBlock {
    private int questionNumber;
    private String studentAnswer;
    private String correctAnswer;
    private boolean isCorrect;

    public AnswerComparisonBlock(int questionNumber, String studentAnswer, String correctAnswer, boolean isCorrect) {
        this.questionNumber = questionNumber;
        this.studentAnswer = studentAnswer;
        this.correctAnswer = correctAnswer;
        this.isCorrect = isCorrect;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public String getStudentAnswer() {
        return studentAnswer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public boolean isCorrect() {
        return isCorrect;
    }
}
