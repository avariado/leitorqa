package com.avariado.leitorqa;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
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
    private SpannableStringBuilder spannable;
    private int lastHighlightStart = -1;
    private int lastHighlightEnd = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // ... (restante do código onCreate permanece o mesmo)
    }

    // ... (outros métodos permanecem iguais até performSearch)

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
            goToSearchResult(currentSearchIndex);
        }
    }

    private void goToSearchResult(int resultIndex) {
        if (searchResults.isEmpty() || resultIndex < 0 || resultIndex >= searchResults.size()) {
            return;
        }
        
        currentSearchIndex = resultIndex;
        currentIndex = searchResults.get(currentSearchIndex);
        updateDisplay();
        highlightSearchTerm();
        updateSearchInfo();
    }

    private void highlightSearchTerm() {
        if (searchTerm.isEmpty() || currentIndex < 0 || currentIndex >= items.size()) {
            return;
        }

        QAItem currentItem = items.get(currentIndex);
        String textToHighlight = isQAMode ? 
            currentItem.getQuestion() + " " + currentItem.getAnswer() : currentItem.getText();
            
        // Remove previous highlights
        if (lastHighlightStart != -1 && lastHighlightEnd != -1 && spannable != null) {
            spannable.removeSpan(new BackgroundColorSpan(Color.YELLOW));
        }

        // Create new spannable with highlights
        spannable = new SpannableStringBuilder(textToHighlight);
        String lowerText = textToHighlight.toLowerCase();
        String lowerSearchTerm = searchTerm.toLowerCase();
        int index = lowerText.indexOf(lowerSearchTerm);
        
        while (index >= 0) {
            spannable.setSpan(new BackgroundColorSpan(Color.YELLOW), 
                           index, index + searchTerm.length(), 
                           Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            index = lowerText.indexOf(lowerSearchTerm, index + searchTerm.length());
        }

        // Apply to the appropriate TextView
        if (isQAMode) {
            questionTextView.setText(spannable);
            answerTextView.setText(currentItem.getAnswer());
        } else {
            questionTextView.setText(spannable);
        }
    }

    private void goToPrevSearchResult() {
        if (searchResults.isEmpty()) return;
        
        int newIndex = (currentSearchIndex - 1 + searchResults.size()) % searchResults.size();
        goToSearchResult(newIndex);
    }
    
    private void goToNextSearchResult() {
        if (searchResults.isEmpty()) return;
        
        int newIndex = (currentSearchIndex + 1) % searchResults.size();
        goToSearchResult(newIndex);
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
        
        // Remove highlights
        if (spannable != null) {
            spannable.removeSpan(new BackgroundColorSpan(Color.YELLOW));
        }
        
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
                    loadedOriginalItems.add(new QAItem(parts[0].trim(), parts[1].trim()));
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
        
        public QAItem(String text) {
            this.text = text;
        }
        
        public QAItem(String question, String answer) {
            this.question = question;
            this.answer = answer;
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
}
