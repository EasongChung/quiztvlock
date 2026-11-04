package com.quiztv.lock;

/**
 * 一道选择题。全部题型统一为 4 选 1，遥控器上下选择、OK 提交。
 */
public class Question {
    public String id;
    public String stem;      // 题干
    public String[] options; // 4 个选项
    public int answer;       // 正确选项下标 0..3
    public String subject;   // 语文 / 数学 / 英语

    public Question(String id, String stem, String[] options, int answer, String subject) {
        this.id = id;
        this.stem = stem;
        this.options = options;
        this.answer = answer;
        this.subject = subject;
    }
}
