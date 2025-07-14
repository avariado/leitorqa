package com.avariado.leitorqa;

import java.util.regex.Pattern;

public class QAItem {
    private final String rawLine;    // Linha original EXATA do arquivo
    private String question;         // Pergunta (com espaços preservados)
    private String answer;           // Resposta (com espaços preservados) 
    private String text;             // Para modo não-Q&A
    private final String separator;  // Delimitador original (;;, ::, \t)
    private final boolean spaceBefore; // Espaço antes do separador?
    private final boolean spaceAfter;  // Espaço depois do separador?

    // Construtor para Q&A (PRESERVA FORMATAÇÃO)
    public QAItem(String rawLine, String separator) {
        this.rawLine = rawLine;
        this.separator = separator;
        
        // Detecta espaços ao redor do separador
        String[] parts = rawLine.split("\\s*" + Pattern.quote(separator) + "\\s*");
        this.spaceBefore = rawLine.substring(0, rawLine.indexOf(separator)).matches(".*\\s$");
        this.spaceAfter = rawLine.substring(rawLine.indexOf(separator) + separator.length()).matches("^\\s.*");
        
        if (parts.length >= 2) {
            this.question = parts[0].trim();
            this.answer = parts[1].trim();
        }
    }

    // Construtor para texto normal
    public QAItem(String text) {
        this.rawLine = text;
        this.text = text;
        this.separator = null;
        this.spaceBefore = false;
        this.spaceAfter = false;
    }

    // GETTERS (NUNCA FAZEM TRIM!)
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public String getText() { return text; }
    public String getSeparator() { return separator; }
    
    // Retorna a linha ORIGINAL com espaços preservados
    public String getOriginalFormat() {
        return rawLine;
    }

    // Verifica se é Q&A
    public boolean isQA() {
        return question != null && answer != null;
    }

    // DEBUG
    @Override
    public String toString() {
        return isQA() ? 
            String.format("Q: '%s' %s '%s' (Original: '%s')", 
                question, separator, answer, rawLine) :
            "TEXT: '" + text + "'";
    }
}
