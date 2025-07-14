package com.avariado.leitorqa;

public class QAItem {
    private String question;
    private String answer;
    private String text;
    private String originalLine;
    
    // Construtor para itens Q&A
    public QAItem(String question, String answer, String originalLine) {
        this.question = question;
        this.answer = answer;
        this.originalLine = originalLine;
    }
    
    // Construtor para texto simples
    public QAItem(String text, String originalLine) {
        this.text = text;
        this.originalLine = originalLine;
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
    
    public String getOriginalLine() {
        return originalLine;
    }
    
    public void setOriginalLine(String originalLine) {
        this.originalLine = originalLine;
    }
    
    public boolean isQA() {
        return question != null && answer != null;
    }
}
