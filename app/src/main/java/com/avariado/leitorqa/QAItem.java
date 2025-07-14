package com.avariado.leitorqa;

public class QAItem {
    private String question;
    private String answer;
    private String text;
    
    public QAItem(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }
    
    public QAItem(String text) {
        this.text = text;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public boolean isQA() {
        return question != null && answer != null;
    }
}
