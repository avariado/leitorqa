package com.avariado.leitorqa;

public class QAItem {
    private String question;
    private String answer;
    private String text;
    private String separator;
    
    // Construtor principal para Q&A
    public QAItem(String question, String answer, String separator) {
        this.question = question;
        this.answer = answer;
        this.separator = separator;
    }
    
    // Construtor para Q&A com separador padrão (backward compatibility)
    public QAItem(String question, String answer) {
        this(question, answer, "\t"); // Chama o construtor principal
    }
    
    // Construtor para texto normal
    public QAItem(String text) {
        this.text = text;
    }
    
    // Getters e Setters
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public String getText() { return text; }
    public String getSeparator() { return separator; }
    public void setText(String text) { this.text = text; }
    
    public boolean isQA() {
        return question != null && answer != null;
    }
}
