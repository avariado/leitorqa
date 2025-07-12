package com.avariado.leitorqa;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // Constantes para SharedPreferences
    private static final String PREFS_NAME = "AppPrefs";
    private static final String ITEMS_KEY = "saved_items";
    private static final String ORIGINAL_ITEMS_KEY = "original_items";
    private static final String CURRENT_INDEX_KEY = "current_index";
    private static final String FONT_SIZE_KEY = "font_size";

    // Constantes para Intents
    private static final int PICK_TXT_FILE = 1001;
    private static final int CREATE_FILE = 1002;
    
    // Constante para swipe
    private static final int MIN_SWIPE_DISTANCE = 150;

    // Views
    private TextView questionTextView, answerTextView, totalCardsText, searchInfo, fontSizeText;
    private EditText currentCardInput, searchInput;
    private CardView cardView;
    private LinearLayout menuLayout;
    private View overlay;

    // Dados
    private List<QAItem> items = new ArrayList<>();
    private List<QAItem> originalItems = new ArrayList<>();
    private int currentIndex = 0;
    private int baseFontSize = 20;
    private boolean menuVisible = false;
    private float startX;

    // Search
    private List<Integer> searchResults = new ArrayList<>();
    private int currentSearchIndex = -1;
    private String searchTerm = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupListeners();
        loadState();
        
        if (items.isEmpty()) {
            loadSampleData();
        }
        
        updateDisplay();
        updateFontSize();
    }

    private void initViews() {
        questionTextView = findViewById(R.id.question_text);
        answerTextView = findViewById(R.id.answer_text);
        currentCardInput = findViewById(R.id.current_card_input);
        totalCardsText = findViewById(R.id.total_cards_text);
        cardView = findViewById(R.id.card_view);
        menuLayout = findViewById(R.id.menu_layout);
        overlay = findViewById(R.id.overlay);
        searchInput = findViewById(R.id.search_input);
        searchInfo = findViewById(R.id.search_info);
        fontSizeText = findViewById(R.id.current_font_size);
    }

    private void setupListeners() {
        cardView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        return true;
                    case MotionEvent.ACTION_UP:
                        handleTouchEvent(event);
                        return true;
                }
                return false;
            }
        });

        findViewById(R.id.menu_button).setOnClickListener(v -> toggleMenu());
        findViewById(R.id.prev_button).setOnClickListener(v -> prevItem());
        findViewById(R.id.next_button).setOnClickListener(v -> nextItem());
        findViewById(R.id.import_button).setOnClickListener(v -> importTextFile());
        findViewById(R.id.export_button).setOnClickListener(v -> showExportDialog());
        findViewById(R.id.edit_button).setOnClickListener(v -> showEditDialog());
        findViewById(R.id.shuffle_button).setOnClickListener(v -> shuffleItems());
        findViewById(R.id.reset_button).setOnClickListener(v -> resetOrder());
        findViewById(R.id.increase_font_button).setOnClickListener(v -> increaseFontSize());
        findViewById(R.id.decrease_font_button).setOnClickListener(v -> decreaseFontSize());
        findViewById(R.id.search_prev_button).setOnClickListener(v -> goToPrevSearchResult());
        findViewById(R.id.search_next_button).setOnClickListener(v -> goToNextSearchResult());
        
        overlay.setOnClickListener(v -> {
            if (menuVisible) toggleMenu();
        });
        
        currentCardInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateAndUpdateCardNumber();
        });
        
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchTerm = s.toString().trim();
                if (searchTerm.isEmpty()) {
                    clearSearch();
                } else {
                    performSearch();
                }
            }
        });
    }

    private void handleTouchEvent(MotionEvent event) {
        float endX = event.getX();
        if (Math.abs(endX - startX) > MIN_SWIPE_DISTANCE) {
            if (endX > startX) {
                prevItem(); // Swipe para direita
            } else {
                nextItem(); // Swipe para esquerda
            }
        } else {
            toggleAnswerVisibility(); // Toque simples
        }
    }

    private void toggleMenu() {
        menuVisible = !menuVisible;
        menuLayout.setVisibility(menuVisible ? View.VISIBLE : View.GONE);
        overlay.setVisibility(menuVisible ? View.VISIBLE : View.GONE);
    }

    private void toggleAnswerVisibility() {
        answerTextView.setVisibility(
            answerTextView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
        );
    }

    private void prevItem() {
        if (!items.isEmpty()) {
            currentIndex = (currentIndex - 1 + items.size()) % items.size();
            updateDisplay();
            saveState();
        }
    }

    private void nextItem() {
        if (!items.isEmpty()) {
            currentIndex = (currentIndex + 1) % items.size();
            updateDisplay();
            saveState();
        }
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
                showToast("Número de cartão inválido");
            }
        } catch (NumberFormatException e) {
            currentCardInput.setText(String.valueOf(currentIndex + 1));
            showToast("Insira um número válido");
        }
    }

    private void updateDisplay() {
        if (items.isEmpty()) {
            questionTextView.setText("Nenhum conteúdo carregado");
            answerTextView.setText("");
            currentCardInput.setText("0");
            totalCardsText.setText("/ 0");
            return;
        }

        QAItem current = items.get(currentIndex);
        questionTextView.setText(current.getQuestion());
        answerTextView.setText(current.getAnswer());
        answerTextView.setVisibility(View.GONE);
        currentCardInput.setText(String.valueOf(currentIndex + 1));
        totalCardsText.setText("/ " + items.size());
    }

    private void updateFontSize() {
        questionTextView.setTextSize(baseFontSize);
        answerTextView.setTextSize(baseFontSize - 2);
        currentCardInput.setTextSize(baseFontSize - 2);
        totalCardsText.setTextSize(baseFontSize - 2);
        fontSizeText.setText(String.valueOf(baseFontSize));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void loadSampleData() {
        String sampleData = "O que é HTML?\tHTML é a linguagem de marcação padrão para criar páginas web.\n" +
                          "O que é CSS?\tCSS é a linguagem de estilos usada para descrever a apresentação de um documento HTML.";
        parseQAContent(sampleData);
    }

    private void parseQAContent(String text) {
        if (text == null) return;
        
        items.clear();
        originalItems.clear();
        
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            
            String separator = line.contains("\t") ? "\t" : ";;";
            String[] parts = line.split(separator);
            
            if (parts.length >= 2) {
                items.add(new QAItem(parts[0].trim(), parts[1].trim()));
            } else {
                items.add(new QAItem(line.trim(), ""));
            }
        }
        
        originalItems = new ArrayList<>(items);
        currentIndex = 0;
        saveState();
    }

    private void importTextFile() {
        toggleMenu();
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_TXT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                String content = readTextFile(uri);
                
                if (requestCode == PICK_TXT_FILE) {
                    parseQAContent(content);
                    updateDisplay();
                    showToast("Ficheiro importado com sucesso!");
                } else if (requestCode == CREATE_FILE) {
                    exportFile(uri);
                }
            } catch (IOException e) {
                showToast("Erro ao ler ficheiro: " + e.getMessage());
            }
        }
    }

    private String readTextFile(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        
        reader.close();
        return stringBuilder.toString();
    }

    private void showExportDialog() {
        toggleMenu();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exportar Ficheiro");
        
        final EditText input = new EditText(this);
        input.setText("perguntas_respostas.txt");
        builder.setView(input);
        
        builder.setPositiveButton("Exportar", (dialog, which) -> {
            String filename = input.getText().toString().trim();
            if (!filename.toLowerCase().endsWith(".txt")) {
                filename += ".txt";
            }
            
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, filename);
            startActivityForResult(intent, CREATE_FILE);
        });
        
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void exportFile(Uri uri) {
        if (items.isEmpty()) return;
        
        StringBuilder content = new StringBuilder();
        for (QAItem item : items) {
            content.append(item.getQuestion()).append("\t").append(item.getAnswer()).append("\n");
        }
        
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            outputStream.write(content.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.close();
            showToast("Ficheiro exportado com sucesso!");
        } catch (IOException e) {
            showToast("Erro ao exportar: " + e.getMessage());
        }
    }

    private void showEditDialog() {
        toggleMenu();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Conteúdo");
        
        final EditText input = new EditText(this);
        input.setMinLines(10);
        
        StringBuilder content = new StringBuilder();
        for (QAItem item : items) {
            content.append(item.getQuestion()).append("\t").append(item.getAnswer()).append("\n");
        }
        input.setText(content.toString());
        
        builder.setView(input);
        
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String text = input.getText().toString();
            if (!text.trim().isEmpty()) {
                parseQAContent(text);
                updateDisplay();
            }
        });
        
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void shuffleItems() {
        if (!items.isEmpty()) {
            Collections.shuffle(items);
            currentIndex = 0;
            updateDisplay();
            toggleMenu();
            saveState();
        }
    }

    private void resetOrder() {
        if (!originalItems.isEmpty()) {
            items = new ArrayList<>(originalItems);
            currentIndex = 0;
            updateDisplay();
            toggleMenu();
            saveState();
        }
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

    private void performSearch() {
        searchResults.clear();
        searchTerm = searchInput.getText().toString().trim().toLowerCase();
        
        if (searchTerm.isEmpty()) {
            clearSearch();
            return;
        }
        
        for (int i = 0; i < items.size(); i++) {
            QAItem item = items.get(i);
            String text = item.getQuestion().toLowerCase() + " " + item.getAnswer().toLowerCase();
            if (text.contains(searchTerm)) {
                searchResults.add(i);
            }
        }
        
        if (searchResults.isEmpty()) {
            searchInfo.setText("Nenhum resultado para: " + searchTerm);
            currentSearchIndex = -1;
        } else {
            currentSearchIndex = 0;
            goToSearchResult(currentSearchIndex);
        }
    }

    private void goToSearchResult(int index) {
        if (index >= 0 && index < searchResults.size()) {
            currentIndex = searchResults.get(index);
            updateDisplay();
            searchInfo.setText((index + 1) + "/" + searchResults.size());
        }
    }

    private void goToPrevSearchResult() {
        if (!searchResults.isEmpty()) {
            currentSearchIndex = (currentSearchIndex - 1 + searchResults.size()) % searchResults.size();
            goToSearchResult(currentSearchIndex);
        }
    }

    private void goToNextSearchResult() {
        if (!searchResults.isEmpty()) {
            currentSearchIndex = (currentSearchIndex + 1) % searchResults.size();
            goToSearchResult(currentSearchIndex);
        }
    }

    private void clearSearch() {
        searchResults.clear();
        currentSearchIndex = -1;
        searchInfo.setText("");
        updateDisplay();
    }

    private void saveState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        StringBuilder sb = new StringBuilder();
        for (QAItem item : items) {
            sb.append(item.getQuestion()).append("\t").append(item.getAnswer()).append("\n");
        }
        editor.putString(ITEMS_KEY, sb.toString());
        
        sb = new StringBuilder();
        for (QAItem item : originalItems) {
            sb.append(item.getQuestion()).append("\t").append(item.getAnswer()).append("\n");
        }
        editor.putString(ORIGINAL_ITEMS_KEY, sb.toString());
        
        editor.putInt(CURRENT_INDEX_KEY, currentIndex);
        editor.putInt(FONT_SIZE_KEY, baseFontSize);
        editor.apply();
    }

    private void loadState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String itemsStr = prefs.getString(ITEMS_KEY, "");
        String originalStr = prefs.getString(ORIGINAL_ITEMS_KEY, "");
        
        if (!itemsStr.isEmpty()) parseQAContent(itemsStr);
        if (!originalStr.isEmpty()) {
            originalItems.clear();
            String[] lines = originalStr.split("\n");
            for (String line : lines) {
                if (line.contains("\t")) {
                    String[] parts = line.split("\t");
                    if (parts.length >= 2) {
                        originalItems.add(new QAItem(parts[0], parts[1]));
                    }
                }
            }
        }
        
        currentIndex = prefs.getInt(CURRENT_INDEX_KEY, 0);
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
        private final String question;
        private final String answer;

        public QAItem(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }

        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
    }
}
