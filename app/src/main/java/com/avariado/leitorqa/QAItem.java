package com.avariado.leitorqa;

import java.util.regex.Pattern;

public class QAItem {
    private String rawLine;          // Linha original exatamente como foi importada
    private String question;         // Pergunta (sem trim para preservar espaços)
    private String answer;           // Resposta (sem trim para preservar espaços)
    private String text;             // Texto normal (para modo não Q&A)
    private String separator;        // Delimitador original (;;, ::, \t)
    private boolean isSpaceBefore;   // Flag para espaço antes do delimitador
    private boolean isSpaceAfter;    // Flag para espaço depois do delimitador

    // Construtor para itens Q&A (com preservação de espaços)
    public QAItem(String rawLine, String separator) {
        this.rawLine = rawLine;
        this.separator = separator;
        
        // Detecta espaços ao redor do delimitador
        String regex = "(.*?)(\\s*)" + Pattern.quote(separator) + "(\\s*)(.*)";
        if (rawLine.matches(regex)) {
            String[] parts = rawLine.split(regex);
            this.isSpaceBefore = !parts[2].isEmpty();
            this.isSpaceAfter = !parts[3].isEmpty();
            
            // Preserva os espaços originais
            this.question = parts[1] + parts[2];
            this.answer = parts[3] + parts[4];
        } else {
            // Fallback se o regex falhar
            String[] parts = rawLine.split(Pattern.quote(separator));
            this.question = parts.length > 0 ? parts[0] : "";
            this.answer = parts.length > 1 ? parts[1] : "";
        }
    }

    // Construtor para texto normal
    public QAItem(String text) {
        this.text = text;
        this.rawLine = text;
    }

    // Método para reconstruir a linha exatamente como foi importada
    public String getOriginalFormat() {
        if (isQA()) {
            StringBuilder sb = new StringBuilder();
            sb.append(question != null ? question : "");
            
            if (isSpaceBefore) sb.append(" ");
            sb.append(separator);
            if (isSpaceAfter) sb.append(" ");
            
            sb.append(answer != null ? answer : "");
            return sb.toString();
        }
        return rawLine != null ? rawLine : text;
    }

    // Getters
    public String getQuestion() { 
        return question != null ? question : ""; 
    }
    
    public String getAnswer() { 
        return answer != null ? answer : ""; 
    }
    
    public String getText() { 
        return text != null ? text : ""; 
    }
    
    public String getSeparator() { 
        return separator != null ? separator : ""; 
    }
    
    public boolean hasSpaceBeforeSeparator() {
        return isSpaceBefore;
    }
    
    public boolean hasSpaceAfterSeparator() {
        return isSpaceAfter;
    }

    // Verifica se é um item Q&A
    public boolean isQA() {
        return question != null && answer != null && separator != null;
    }

    // Setters (com preservação de espaços)
    public void setQuestion(String question) {
        this.question = question;
        updateRawLine();
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
        updateRawLine();
    }
    
    public void setSeparator(String separator) {
        this.separator = separator;
        updateRawLine();
    }
    
    public void setText(String text) {
        this.text = text;
        this.rawLine = text;
    }

    // Atualiza a linha raw quando os dados mudam
    private void updateRawLine() {
        if (isQA()) {
            this.rawLine = getOriginalFormat();
        }
    }

    @Override
    public String toString() {
        return isQA() ? "QAItem[" + getOriginalFormat() + "]" 
                     : "TextItem[" + rawLine + "]";
    }
}
