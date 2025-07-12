package com.avariado.leitorqa;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_TXT_FILE = 1;
    private static final int CREATE_FILE = 2;
    
    private static final String PREFS_NAME = "AppPrefs";
    private static final String ITEMS_KEY = "items";
    private static final String ORIGINAL_ITEMS_KEY = "originalItems";
    private static final String CURRENT_INDEX_KEY = "currentIndex";
    private static final String IS_QA_MODE_KEY = "isQAMode";
    private static final String FONT_SIZE_KEY = "fontSize";

    // Views
    private TextView questionTextView;
    private TextView answerTextView;
    private EditText currentCardInput;
    private TextView totalCardsText;
    private LinearLayout menuLayout;
    private View overlay;
    private EditText searchInput;
    private TextView searchInfo;
    private TextView fontSizeText;
    
    // Data
    private List<QAItem> items = new ArrayList<>();
    private List<QAItem> originalItems = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isQAMode = true;
    private boolean menuVisible = false;
    private int baseFontSize = 20;
    
    // Search
    private List<Integer> searchResults = new ArrayList<>();
    private int currentSearchIndex = -1;
    private String searchTerm = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Inicialização de views (mantido igual)
        questionTextView = findViewById(R.id.question_text);
        answerTextView = findViewById(R.id.answer_text);
        currentCardInput = findViewById(R.id.current_card_input);
        totalCardsText = findViewById(R.id.total_cards_text);
        menuLayout = findViewById(R.id.menu_layout);
        overlay = findViewById(R.id.overlay);
        searchInput = findViewById(R.id.search_input);
        searchInfo = findViewById(R.id.search_info);
        fontSizeText = findViewById(R.id.current_font_size);
        
        // Inicialização de botões (mantido igual)
        Button menuButton = findViewById(R.id.menu_button);
        Button prevButton = findViewById(R.id.prev_button);
        Button nextButton = findViewById(R.id.next_button);
        Button importButton = findViewById(R.id.import_button);
        Button exportButton = findViewById(R.id.export_button);
        Button editButton = findViewById(R.id.edit_button);
        Button shuffleButton = findViewById(R.id.shuffle_button);
        Button resetButton = findViewById(R.id.reset_button);
        Button increaseFontButton = findViewById(R.id.increase_font_button);
        Button decreaseFontButton = findViewById(R.id.decrease_font_button);
        Button searchPrevButton = findViewById(R.id.search_prev_button);
        Button searchNextButton = findViewById(R.id.search_next_button);
        
        // Listeners (mantido igual)
        RelativeLayout mainContentArea = findViewById(R.id.main_content_area);
        mainContentArea.setOnClickListener(v -> toggleAnswerVisibility());
        
        menuButton.setOnClickListener(v -> toggleMenu());
        prevButton.setOnClickListener(v -> safePrevItem());
        nextButton.setOnClickListener(v -> safeNextItem());
        importButton.setOnClickListener(v -> importTextFile());
        exportButton.setOnClickListener(v -> showExportDialog());
        editButton.setOnClickListener(v -> showEditDialog());
        shuffleButton.setOnClickListener(v -> shuffleItems());
        resetButton.setOnClickListener(v -> resetOrder());
        increaseFontButton.setOnClickListener(v -> increaseFontSize());
        decreaseFontButton.setOnClickListener(v -> decreaseFontSize());
        searchPrevButton.setOnClickListener(v -> goToPrevSearchResult());
        searchNextButton.setOnClickListener(v -> goToNextSearchResult());
        
        overlay.setOnClickListener(v -> {
            if (menuVisible) toggleMenu();
        });
        
        currentCardInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateAndUpdateCardNumber();
        });
        
        currentCardInput.setOnEditorActionListener((v, actionId, event) -> {
            validateAndUpdateCardNumber();
            return true;
        });
        
        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchTerm = s.toString().trim();
                if (searchTerm.isEmpty()) clearSearch();
                else performSearch();
            }
            public void afterTextChanged(Editable s) {}
        });
        
        loadState();
        if (items.isEmpty()) loadSampleData();
        updateDisplay();
        updateFontSize();
    }

    // Métodos auxiliares (mantidos iguais)
    private void safePrevItem() {
        try { prevItem(); } catch (Exception e) { showError("Erro ao navegar"); }
    }
    
    private void safeNextItem() {
        try { nextItem(); } catch (Exception e) { showError("Erro ao navegar"); }
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void validateAndUpdateCardNumber() {
        try {
            String input = currentCardInput.getText().toString().trim();
            if (input.isEmpty()) {
                currentCardInput.setText(String.valueOf(currentIndex + 1));
                return;
            }
            
            int num = Integer.parseInt(input);
            if (num >= 1 && num <= items.size()) {
                currentIndex = num - 1;
                updateDisplay();
                saveState();
            } else {
                currentCardInput.setText(String.valueOf(currentIndex + 1));
                showError("Número inválido");
            }
        } catch (NumberFormatException e) {
            currentCardInput.setText(String.valueOf(currentIndex + 1));
            showError("Insira um número válido");
        }
    }
    
    private void toggleMenu() {
        menuVisible = !menuVisible;
        menuLayout.setVisibility(menuVisible ? View.VISIBLE : View.GONE);
        overlay.setVisibility(menuVisible ? View.VISIBLE : View.GONE);
    }
    
    private void toggleAnswerVisibility() {
        if (answerTextView.getVisibility() == View.VISIBLE) {
            answerTextView.setVisibility(View.GONE);
        } else {
            answerTextView.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateDisplay() {
        if (items.isEmpty()) {
            questionTextView.setText("Nenhum conteúdo");
            answerTextView.setText("");
            currentCardInput.setText("0");
            totalCardsText.setText("/ 0");
            return;
        }
        
        currentIndex = Math.max(0, Math.min(currentIndex, items.size() - 1));
        QAItem currentItem = items.get(currentIndex);
        
        if (isQAMode) {
            questionTextView.setText(currentItem.getQuestion());
            answerTextView.setText(currentItem.getAnswer());
            answerTextView.setVisibility(View.GONE);
        } else {
            questionTextView.setText(currentItem.getText());
            answerTextView.setText("");
            answerTextView.setVisibility(View.GONE);
        }
        
        currentCardInput.setText(String.valueOf(currentIndex + 1));
        totalCardsText.setText("/ " + items.size());
    }

    private void prevItem() {
        if (items.isEmpty()) return;
        currentIndex = (currentIndex - 1 + items.size()) % items.size();
        updateDisplay();
        saveState();
    }
    
    private void nextItem() {
        if (items.isEmpty()) return;
        currentIndex = (currentIndex + 1) % items.size();
        updateDisplay();
        saveState();
    }
    
    private void shuffleItems() {
        if (items.isEmpty()) return;
        Collections.shuffle(items);
        currentIndex = 0;
        updateDisplay();
        toggleMenu();
        saveState();
    }
    
    private void resetOrder() {
        if (originalItems.isEmpty()) return;
        items = new ArrayList<>(originalItems);
        currentIndex = 0;
        updateDisplay();
        toggleMenu();
        saveState();
    }
    
    private void increaseFontSize() {
        baseFontSize = Math.min(baseFontSize + 2, 32);
        updateFontSize();
        saveState();
    }
    
    private void decreaseFontSize() {
        baseFontSize = Math.max(baseFontSize - 2, 12);
        updateFontSize();
        saveState();
    }
    
    private void updateFontSize() {
        questionTextView.setTextSize(baseFontSize);
        answerTextView.setTextSize(baseFontSize - 2);
        currentCardInput.setTextSize(baseFontSize - 2);
        totalCardsText.setTextSize(baseFontSize - 2);
        fontSizeText.setText(String.valueOf(baseFontSize));
    }
    
    private void loadSampleData() {
        try {
            InputStream is = getAssets().open("sample_qa.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            parseQAContent(sb.toString());
        } catch (IOException e) {
            String sampleData = "Exemplo Q?\tExemplo A\nOutra Q?\tOutra A";
            parseQAContent(sampleData);
        }
    }
    
    private void parseQAContent(String text) {
        if (text == null) return;
        
        String[] lines = text.split("\n");
        items.clear();
        originalItems.clear();
        
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            
            String separator = line.contains("\t") ? "\t" : ";;";
            String[] parts = line.split(separator);
            
            if (parts.length >= 2) {
                items.add(new QAItem(parts[0].trim(), parts[1].trim()));
            } else {
                items.add(new QAItem(line.trim()));
            }
        }
        
        originalItems = new ArrayList<>(items);
        currentIndex = 0;
        isQAMode = !items.isEmpty() && items.get(0).isQA();
    }

    // MÉTODO COMPLETO DE PROCESSAMENTO DE TEXTO
    private void parseTextContent(String text) {
        if (text == null || text.isEmpty()) return;

        // 1. Remove TODAS as quebras de linha
        String singleLine = text.replace("\r\n", " ")
                              .replace("\n", " )
                              .replace("\r", " ")
                              .replaceAll("\\s+", " )
                              .trim();

        // 2. Divisão em frases completas
        List<QAItem> processedItems = new ArrayList<>();
        StringBuilder currentSentence = new StringBuilder();
        int charCount = 0;
        boolean inQuotes = false;

        for (int i = 0; i < singleLine.length(); i++) {
            char c = singleLine.charAt(i);
            currentSentence.append(c);
            charCount++;

            if (c == '"' || c == '\'') inQuotes = !inQuotes;

            if (!inQuotes && (c == '.' || c == '!' || c == '?' || 
                (c == '.' && i+2 < singleLine.length() && 
                 singleLine.substring(i, i+3).equals("...")))) {
                
                if (c == '.' && i+2 < singleLine.length() && 
                    singleLine.substring(i, i+3).equals("...")) {
                    currentSentence.append("..");
                    i += 2;
                }

                if (charCount >= 75 || i == singleLine.length()-1) {
                    processedItems.add(new QAItem(currentSentence.toString().trim()));
                    currentSentence = new StringBuilder();
                    charCount = 0;
                }
            }
        }

        items = processedItems;
        originalItems = new ArrayList<>(items);
        currentIndex = 0;
        isQAMode = false;
    }

    // MÉTODO COMPLETO DE LEITURA COM DETECÇÃO DE ENCODING
    private String readTextFileWithEncodingDetection(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        bis.mark(100000);

        // 1. Tenta UTF-8
        try {
            String content = readStream(bis, StandardCharsets.UTF_8);
            if (!hasInvalidUTF8Characters(content)) {
                return content;
            }
        } catch (Exception e) {
            Log.e("ENCODING", "UTF-8 failed", e);
        }

        // 2. Tenta Windows-1252 (ANSI)
        try {
            bis.reset();
            return readStream(bis, Charset.forName("Windows-1252"));
        } catch (Exception e) {
            Log.e("ENCODING", "ANSI failed", e);
        }

        // 3. Fallback para ISO-8859-1
        try {
            bis.reset();
            return readStream(bis, StandardCharsets.ISO_8859_1);
        } finally {
            bis.close();
        }
    }

    // MÉTODO DE VERIFICAÇÃO DE UTF-8 (RESTAURADO)
    private boolean hasInvalidUTF8Characters(String content) {
        return content.contains("�");
    }

    private String readStream(InputStream is, Charset charset) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                String fileContent = readTextFileWithEncodingDetection(uri);
                parseTextContent(fileContent);
                updateDisplay();
                saveState();
                Toast.makeText(this, "Ficheiro importado!", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("FILE_IMPORT", "Error", e);
            }
        }
    }

    private void showExportDialog() {
        toggleMenu();
        
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.export_dialog, null);
        EditText filenameInput = dialogView.findViewById(R.id.export_filename);
        filenameInput.setText(isQAMode ? "perguntas_respostas.txt" : "documento.txt");
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle("Exportar Ficheiro");
        builder.setPositiveButton("Exportar", (dialog, which) -> {
            String filename = filenameInput.getText().toString().trim();
            if (filename.isEmpty()) {
                filename = isQAMode ? "perguntas_respostas.txt" : "documento.txt";
            }
            if (!filename.toLowerCase().endsWith(".txt")) {
                filename += ".txt";
            }
            
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, filename);
            startActivityForResult(intent, CREATE_FILE);
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    
    private void exportFile(Uri uri) {
        if (items.isEmpty()) return;
        
        StringBuilder content = new StringBuilder();
        if (isQAMode) {
            for (QAItem item : items) {
                content.append(item.getQuestion()).append("\t").append(item.getAnswer()).append("\n");
            }
        } else {
            for (QAItem item : items) {
                content.append(item.getText()).append("\n");
            }
        }
        
        try {
            OutputStream fos = getContentResolver().openOutputStream(uri);
            fos.write(content.toString().getBytes());
            fos.close();
            
            Toast.makeText(this, "Ficheiro exportado com sucesso!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Erro ao exportar ficheiro: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void showEditDialog() {
        toggleMenu();
        
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.edit_dialog, null);
        EditText contentEditor = dialogView.findViewById(R.id.content_editor);
        
        StringBuilder content = new StringBuilder();
        if (isQAMode) {
            for (QAItem item : items) {
                content.append(item.getQuestion()).append("\t").append(item.getAnswer()).append("\n");
            }
        } else {
            for (QAItem item : items) {
                content.append(item.getText()).append("\n");
            }
        }
        contentEditor.setText(content.toString());
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle("Editar Conteúdo");
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String text = contentEditor.getText().toString();
            if (text.trim().isEmpty()) {
                Toast.makeText(this, "O conteúdo não pode estar vazio!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            boolean hasTabs = text.contains("\t");
            boolean hasDoubleSemicolon = text.contains(";;");
            boolean isAlternatingLines = checkAlternatingLinesFormat(text);
            
            if (hasTabs || hasDoubleSemicolon) {
                parseQAContent(text);
            } else if (isAlternatingLines) {
                parseAlternatingLinesContent(text);
            } else {
                parseTextContent(text);
            }
            
            updateDisplay();
            saveState();
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    
    private boolean checkAlternatingLinesFormat(String text) {
        String[] lines = text.split("\n");
        if (lines.length < 2) return false;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.split("\\.").length > 2 && !trimmedLine.endsWith(".")) {
                return false;
            }
            if ((trimmedLine.replaceAll("[^.!?]", "").length()) > 1) {
                return false;
            }
        }
        
        return true;
    }
    
    private void parseAlternatingLinesContent(String text) {
        String[] lines = text.split("\n");
        items.clear();
        originalItems.clear();
        
        if (lines.length % 2 != 0) {
            Toast.makeText(this, "Aviso: O número de linhas não é par. A última linha será ignorada.", Toast.LENGTH_LONG).show();
        }
        
        for (int i = 0; i < lines.length - 1; i += 2) {
            String question = lines[i].trim();
            String answer = lines[i + 1].trim();
            items.add(new QAItem(question, answer));
        }
        
        originalItems = new ArrayList<>(items);
        currentIndex = 0;
        isQAMode = true;
    }
    
    private void performSearch() {
        searchTerm = searchInput.getText().toString().trim();
        if (searchTerm.isEmpty()) {
            clearSearch();
            return;
        }
        
        searchResults.clear();
        
        for (int i = 0; i < items.size(); i++) {
            QAItem item = items.get(i);
            String textToSearch = isQAMode ? 
                item.getQuestion() + " " + item.getAnswer() : item.getText();
            
            if (textToSearch.toLowerCase().contains(searchTerm.toLowerCase())) {
                searchResults.add(i);
            }
        }
        
        if (searchResults.isEmpty()) {
            searchInfo.setText("Nenhum resultado encontrado para: " + searchTerm);
            currentSearchIndex = -1;
        } else {
            currentSearchIndex = 0;
            currentIndex = searchResults.get(currentSearchIndex);
            updateSearchInfo();
        }
        
        updateDisplay();
    }
    
    private void goToPrevSearchResult() {
        if (searchResults.isEmpty()) return;
        
        currentSearchIndex = (currentSearchIndex - 1 + searchResults.size()) % searchResults.size();
        currentIndex = searchResults.get(currentSearchIndex);
        updateDisplay();
        updateSearchInfo();
    }
    
    private void goToNextSearchResult() {
        if (searchResults.isEmpty()) return;
        
        currentSearchIndex = (currentSearchIndex + 1) % searchResults.size();
        currentIndex = searchResults.get(currentSearchIndex);
        updateDisplay();
        updateSearchInfo();
    }
    
    private void updateSearchInfo() {
        if (searchResults.isEmpty()) {
            searchInfo.setText("");
        } else {
            searchInfo.setText("Resultado " + (currentSearchIndex + 1) + " de " + searchResults.size());
        }
    }
    
    private void clearSearch() {
        searchTerm = "";
        searchResults.clear();
        currentSearchIndex = -1;
        searchInfo.setText("");
        updateDisplay();
    }
    
    private void saveState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        StringBuilder itemsStr = new StringBuilder();
        for (QAItem item : items) {
            if (item.isQA()) {
                itemsStr.append(item.getQuestion()).append("\t").append(item.getAnswer()).append("\n");
            } else {
                itemsStr.append(item.getText()).append("\n");
            }
        }
        editor.putString(ITEMS_KEY, itemsStr.toString());
        
        StringBuilder originalItemsStr = new StringBuilder();
        for (QAItem item : originalItems) {
            if (item.isQA()) {
                originalItemsStr.append(item.getQuestion()).append("\t").append(item.getAnswer()).append("\n");
            } else {
                originalItemsStr.append(item.getText()).append("\n");
            }
        }
        editor.putString(ORIGINAL_ITEMS_KEY, originalItemsStr.toString());
        
        editor.putInt(CURRENT_INDEX_KEY, currentIndex);
        editor.putBoolean(IS_QA_MODE_KEY, isQAMode);
        editor.putInt(FONT_SIZE_KEY, baseFontSize);
        editor.apply();
    }
    
    private void loadState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        String itemsStr = prefs.getString(ITEMS_KEY, "");
        String originalItemsStr = prefs.getString(ORIGINAL_ITEMS_KEY, "");
        
        if (!itemsStr.isEmpty()) {
            parseQAContent(itemsStr);
        }
        
        if (!originalItemsStr.isEmpty()) {
            List<QAItem> loadedOriginalItems = new ArrayList<>();
            String[] lines = originalItemsStr.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                
                String[] parts = line.split("\t");
                if (parts.length >= 2) {
                    loadedOriginalItems.add(new QAItem(parts[0].trim(), parts[1].trim());
                } else {
                    loadedOriginalItems.add(new QAItem(line.trim()));
                }
            }
            originalItems = loadedOriginalItems;
        }
        
        currentIndex = prefs.getInt(CURRENT_INDEX_KEY, 0);
        isQAMode = prefs.getBoolean(IS_QA_MODE_KEY, true);
        baseFontSize = prefs.getInt(FONT_SIZE_KEY, 20);
    }
    
    @Override
    public void onBackPressed() {
        if (menuVisible) {
            toggleMenu();
        } else {
            super.onBackPressed();
        }
    }
    
    private static class QAItem {
        private String question;
        private String answer;
        private String text;
        
        public QAItem(String text) { this.text = text; }
        public QAItem(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
        
        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public boolean isQA() { return question != null && answer != null; }
    }
}
