package com.avariado.leitorqa;

public class QAItem {
    private String question;
    private String answer;
    private String text;
    private String separator;
    private String originalLine;  // Guarda a linha original para preservar formatação
    
    // Construtor para Q&A com separador personalizado
    public QAItem(String question, String answer, String separator) {
        this.question = question != null ? question.trim() : null;
        this.answer = answer != null ? answer.trim() : null;
        this.separator = separator;
        this.originalLine = (question != null ? question : "") + separator + (answer != null ? answer : "");
    }
    
    // Construtor para Q&A com separador padrão (tab)
    public QAItem(String question, String answer) {
        this(question, answer, "\t");
    }
    
    // Construtor para texto normal
    public QAItem(String text) {
        this.text = text != null ? text.trim() : null;
        this.originalLine = text;
    }
    
    // Getters
    public String getQuestion() { 
        return question; 
    }
    
    public String getAnswer() { 
        return answer; 
    }
    
    public String getText() { 
        return text; 
    }
    
    public String getSeparator() { 
        return separator; 
    }
    
    // Retorna a linha exatamente como foi importada
    public String getOriginalFormat() {
        return originalLine != null ? originalLine : 
               isQA() ? question + separator + answer : text;
    }
    
    // Setters
    public void setQuestion(String question) {
        this.question = question != null ? question.trim() : null;
        updateOriginalLine();
    }
    
    public void setAnswer(String answer) {
        this.answer = answer != null ? answer.trim() : null;
        updateOriginalLine();
    }
    
    public void setText(String text) {
        this.text = text != null ? text.trim() : null;
        this.originalLine = text;
    }
    
    public void setSeparator(String separator) {
        this.separator = separator;
        updateOriginalLine();
    }
    
    // Verifica se é um item de Q&A
    public boolean isQA() {
        return question != null && answer != null;
    }
    
    // Atualiza a linha original quando os campos mudam
    private void updateOriginalLine() {
        if (isQA()) {
            this.originalLine = question + separator + answer;
        }
    }
    
    // Representação para debug
    @Override
    public String toString() {
        return isQA() ? "Q: " + question + " | A: " + answer + " | Sep: '" + separator + "'" 
                     : "Text: " + text;
    }
}
