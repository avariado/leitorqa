package com.avariado.leitorqa;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GestureDetectorCompat;

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
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import android.app.ProgressDialog;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_TXT_FILE = 1;
    private static final int CREATE_FILE = 2;
    private static final String HIGHLIGHT_PATTERN = "(?i)(%s)";
    private static final String HIGHLIGHT_COLOR = "#FF5722";
    
    private static final String PREFS_NAME = "AppPrefs";
    private static final String ITEMS_KEY = "items";
    private static final String ORIGINAL_ITEMS_KEY = "originalItems";
    private static final String CURRENT_INDEX_KEY = "currentIndex";
    private static final String IS_QA_MODE_KEY = "isQAMode";
    private static final String FONT_SIZE_KEY = "fontSize";

    private static final int PICK_PDF_FILE = 3;

    private static final float QA_LINE_SPACING_EXTRA = 10f;
    private static final float TEXT_LINE_SPACING_EXTRA = 6f;
    private static final float QA_LINE_SPACING_MULTIPLIER = 1.3f;
    private static final float TEXT_LINE_SPACING_MULTIPLIER = 1.2f;

    private TextView questionTextView;
    private TextView answerTextView;
    private EditText currentCardInput;
    private TextView totalCardsText;
    private LinearLayout menuLayout;
    private View overlay;
    private EditText searchInput;
    private TextView searchInfo;
    private TextView fontSizeText;
    private FrameLayout mainContainer;
    private CardView cardView;
    private ScrollView textScrollView;
    
    private List<QAItem> items = new ArrayList<>();
    private List<QAItem> originalItems = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isQAMode = true;
    private boolean menuVisible = false;
    private int baseFontSize = 20;
    
    private List<Integer> searchResults = new ArrayList<>();
    private int currentSearchIndex = -1;
    private String searchTerm = "";

    private GestureDetectorCompat gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        questionTextView = findViewById(R.id.question_text);
        answerTextView = findViewById(R.id.answer_text);
        currentCardInput = findViewById(R.id.current_card_input);
        totalCardsText = findViewById(R.id.total_cards_text);
        menuLayout = findViewById(R.id.menu_layout);
        overlay = findViewById(R.id.overlay);
        searchInput = findViewById(R.id.search_input);
        searchInfo = findViewById(R.id.search_info);
        fontSizeText = findViewById(R.id.current_font_size);
        mainContainer = findViewById(R.id.main_container);
        cardView = findViewById(R.id.card_view);
        textScrollView = findViewById(R.id.text_scroll_view);

        // Pré-medida do menu (versão corrigida)
        menuLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Remove o listener para não ser chamado novamente
                menuLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                
                // Configura a posição inicial fora da tela
                menuLayout.setX(-menuLayout.getWidth());
                
                // Esconde o menu após a pré-medida
                menuLayout.setVisibility(View.GONE);
            }
        });
        
        // Força o layout a ser medido
        menuLayout.setVisibility(View.VISIBLE);
        
        gestureDetector = new GestureDetectorCompat(this, new SwipeGestureListener());
        
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
        
        currentCardInput.setFocusable(false);
        currentCardInput.setFocusableInTouchMode(false);
        currentCardInput.setCursorVisible(false);
        
        setupCardInputBehavior();

        cardView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });
        
        textScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false;
            }
        });
        
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
            if (menuVisible) {
                toggleMenu();
            }
        });
        
        currentCardInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateAndUpdateCardNumber();
            }
        });
        
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchTerm = s.toString().trim();
                if (searchTerm.isEmpty()) {
                    clearSearch();
                } else {
                    performSearch();
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        loadState();
        if (items.isEmpty()) {
            loadSampleData();
        }
        updateDisplay();
        updateFontSize();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (currentCardInput.hasFocus()) {
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                finishEditing();
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }
    
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                safePrevItem();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                safeNextItem();
                return true;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                toggleAnswerVisibility();
                return true;
            case KeyEvent.KEYCODE_SPACE:
                toggleMenu();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            
            if (currentCardInput.hasFocus() && 
               (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                finishEditing();
                return true;
            }
            
            if (!currentCardInput.hasFocus()) {
                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    toggleAnswerVisibility();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_SPACE) {
                    toggleMenu();
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void setupCardInputBehavior() {
        currentCardInput.setOnClickListener(v -> enableEditing());
        
        currentCardInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                finishEditing();
                return true;
            }
            return false;
        });
        
        cardView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (!menuVisible) {
                    finishEditing();
                    toggleAnswerVisibility();
                }
            }
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void enableEditing() {
        currentCardInput.setFocusable(true);
        currentCardInput.setFocusableInTouchMode(true);
        currentCardInput.requestFocus();
        currentCardInput.setCursorVisible(true);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(currentCardInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void finishEditing() {
        currentCardInput.clearFocus();
        currentCardInput.setFocusable(false);
        currentCardInput.setFocusableInTouchMode(false);
        currentCardInput.setCursorVisible(false);
        validateAndUpdateCardNumber();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(currentCardInput.getWindowToken(), 0);
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        private static final float SWIPE_ANGLE_THRESHOLD = 30;
    
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            toggleAnswerVisibility();
            return true;
        }
    
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();
            
            float angle = (float) Math.toDegrees(Math.atan2(diffY, diffX));
            
            if (Math.abs(angle) < SWIPE_ANGLE_THRESHOLD || 
                Math.abs(angle) > 180 - SWIPE_ANGLE_THRESHOLD) {
                
                if (Math.abs(diffX) > SWIPE_THRESHOLD && 
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    
                    if (diffX > 0) {
                        safePrevItem();
                    } else {
                        safeNextItem();
                    }
                    return true;
                }
            }
            return false;
        }
    }

    private void safePrevItem() {
        try {
            prevItem();
        } catch (Exception e) {
            showError("Erro ao navegar para o cartão anterior");
        }
    }
    
    private void safeNextItem() {
        try {
            nextItem();
        } catch (Exception e) {
            showError("Erro ao navegar para o próximo cartão");
        }
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
                showError("Número de cartão inválido");
            }
        } catch (NumberFormatException e) {
            currentCardInput.setText(String.valueOf(currentIndex + 1));
            showError("Por favor insira um número válido");
        }
    }
    
    private void toggleMenu() {
        menuVisible = !menuVisible;
        
        if (menuVisible) {
            // Se ainda não foi medido (improvável após a pré-medida)
            if (menuLayout.getWidth() <= 0) {
                menuLayout.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                menuLayout.layout(0, 0, menuLayout.getMeasuredWidth(), menuLayout.getMeasuredHeight());
            }
            
            menuLayout.setX(-menuLayout.getWidth());
            menuLayout.setVisibility(View.VISIBLE);
            overlay.setVisibility(View.VISIBLE);
            
            menuLayout.animate()
                .translationX(0)
                .setDuration(300)
                .start();
        } else {
            menuLayout.animate()
                .translationX(-menuLayout.getWidth())
                .setDuration(300)
                .withEndAction(() -> {
                    menuLayout.setVisibility(View.GONE);
                    overlay.setVisibility(View.GONE);
                })
                .start();
        }
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
            questionTextView.setText("Nenhum conteúdo carregado.");
            answerTextView.setText("");
            currentCardInput.setText("0");
            totalCardsText.setText("/ 0");
            return;
        }
    
        if (isQAMode) {
            questionTextView.setLineSpacing(QA_LINE_SPACING_EXTRA, QA_LINE_SPACING_MULTIPLIER);
            answerTextView.setLineSpacing(QA_LINE_SPACING_EXTRA, QA_LINE_SPACING_MULTIPLIER);
        } else {
            questionTextView.setLineSpacing(TEXT_LINE_SPACING_EXTRA, TEXT_LINE_SPACING_MULTIPLIER);
        }
    
        currentIndex = Math.max(0, Math.min(currentIndex, items.size() - 1));
        QAItem currentItem = items.get(currentIndex);
    
        if (isQAMode) {
            questionTextView.setText(highlightText(currentItem.getQuestion(), searchTerm));
            answerTextView.setText(highlightText(currentItem.getAnswer(), searchTerm));
            answerTextView.setVisibility(View.GONE);
        } else {
            questionTextView.setText(highlightText(currentItem.getText(), searchTerm));
            answerTextView.setText("");
            answerTextView.setVisibility(View.GONE);
        }
    
        currentCardInput.setText(String.valueOf(currentIndex + 1));
        totalCardsText.setText("/ " + items.size());
    }

    private Spanned highlightText(String text, String searchTerm) {
        if (text == null || searchTerm == null || searchTerm.isEmpty()) {
            return Html.fromHtml(text != null ? text : "");
        }
        
        String highlighted = text.replaceAll(
            String.format(HIGHLIGHT_PATTERN, Pattern.quote(searchTerm)),
            "<font color='" + HIGHLIGHT_COLOR + "'>$1</font>"
        );
        return Html.fromHtml(highlighted);
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
            String sampleData = "O que é HTML?\tHTML é a linguagem de marcação padrão para criar páginas web.\n" +
                              "O que é CSS?\tCSS é a linguagem de estilos usada para descrever a apresentação de um documento HTML.";
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
                String question = parts[0].trim();
                String answer = parts[1].trim();
                items.add(new QAItem(question, answer, line));
            } else {
                items.add(new QAItem(line.trim(), line));
            }
        }
        
        originalItems = new ArrayList<>(items);
        currentIndex = 0;
        isQAMode = !items.isEmpty() && items.get(0).isQA();
    }
    
    private boolean isSingleSentence(String line) {
        if (line == null || line.trim().isEmpty()) return false;
        
        int punctuationCount = line.replaceAll("[^.!?]", "").length();
        
        return punctuationCount == 1 && 
               (line.endsWith(".") || line.endsWith("!") || line.endsWith("?"));
    }
    
    private void parseTextContent(String text) {
        if (text == null) return;
        
        String originalText = text;
        
        String singleLine = text.replaceAll("[\\r\\n]+", " ")
                              .replaceAll("\\s+", " ")
                              .trim();
        
        Pattern pattern = Pattern.compile("[^.!?]+[.!?]+");
        Matcher matcher = pattern.matcher(singleLine);
        List<String> sentences = new ArrayList<>();
        
        while (matcher.find()) {
            sentences.add(matcher.group().trim());
        }
        
        List<QAItem> processedItems = new ArrayList<>();
        StringBuilder currentSentence = new StringBuilder();
        
        for (String sentence : sentences) {
            if (currentSentence.length() == 0) {
                currentSentence.append(sentence);
            } else if (currentSentence.length() + sentence.length() < 75) {
                currentSentence.append(" ").append(sentence);
            } else {
                processedItems.add(new QAItem(currentSentence.toString(), currentSentence.toString()));
                currentSentence = new StringBuilder(sentence);
            }
        }
        
        if (currentSentence.length() > 0) {
            processedItems.add(new QAItem(currentSentence.toString(), currentSentence.toString()));
        }
        
        items = processedItems;
        if (!processedItems.isEmpty()) {
            processedItems.get(0).setOriginalLine(originalText);
        }
        originalItems = new ArrayList<>(items);
        currentIndex = 0;
        isQAMode = false;
    }
    
    private void importTextFile() {
        toggleMenu();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecionar tipo de ficheiro");
        builder.setItems(new String[]{"Texto (TXT)", "PDF com texto"}, (dialog, which) -> {
            if (which == 0) {
                // TXT file
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("text/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, PICK_TXT_FILE);
            } else {
                // PDF file
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/pdf");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, PICK_PDF_FILE);
            }
        });
        builder.show();
    }

        private void processImportedText(String text, boolean isPdf) {
        runOnUiThread(() -> {
            try {
                // Verificar se o texto parece ser QA (pergunta/resposta)
                boolean isAlternatingQa = true;
                String[] lines = text.split("\n");
                
                // PDFs tendem a ter formatação diferente, então ajustamos a verificação
                if (isPdf) {
                    // Juntar linhas que podem ter sido quebradas erroneamente
                    StringBuilder normalizedText = new StringBuilder();
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty()) continue;
                        
                        // Se a linha termina com pontuação, assumir que é uma frase completa
                        if (trimmed.matches(".*[.!?]\""?\\s*$")) {
                            normalizedText.append(trimmed).append("\n");
                        } else {
                            normalizedText.append(trimmed).append(" ");
                        }
                    }
                    text = normalizedText.toString();
                    lines = text.split("\n");
                }
                
                // Verificar padrão QA
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (!line.isEmpty() && !isSingleSentence(line)) {
                        isAlternatingQa = false;
                        break;
                    }
                }
    
                if (isAlternatingQa && lines.length >= 2 && lines.length % 2 == 0) {
                    parseAlternatingLinesContent(text);
                } else if (text.contains("\t") || text.contains(";;")) {
                    parseQAContent(text);
                } else {
                    parseTextContent(text);
                }
                
                updateDisplay();
                saveState();
            } catch (Exception e) {
                Toast.makeText(this, "Erro ao processar conteúdo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("PROCESS_TEXT", "Erro ao processar texto importado", e);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            
            // Diálogo de progresso que será usado para ambos PDF e TXT
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            
            try {
                if (requestCode == PICK_PDF_FILE) {
                    progressDialog.setMessage("Processando PDF...");
                    progressDialog.show();
                    
                    new Thread(() -> {
                        try {
                            String pdfText = extractTextFromPdf(uri);
                            
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                
                                if (pdfText == null || pdfText.trim().isEmpty()) {
                                    Toast.makeText(this, "Não foi possível extrair texto do PDF", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                
                                // Processar o texto extraído
                                processImportedText(pdfText, true);
                                Toast.makeText(this, "PDF importado com sucesso!", Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(this, "Erro ao processar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e("PDF_IMPORT", "Erro ao processar PDF", e);
                            });
                        }
                    }).start();
                    
                } else if (requestCode == PICK_TXT_FILE) {
                    progressDialog.setMessage("Lendo arquivo de texto...");
                    progressDialog.show();
                    
                    new Thread(() -> {
                        try {
                            String fileContent = readTextFileWithEncodingDetection(uri);
                            
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                
                                if (fileContent == null || fileContent.trim().isEmpty()) {
                                    Toast.makeText(this, "O arquivo está vazio ou não pôde ser lido", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                
                                // Processar o conteúdo do arquivo de texto
                                processImportedText(fileContent, false);
                                Toast.makeText(this, "Arquivo importado com sucesso!", Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(this, "Erro ao ler arquivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e("TXT_IMPORT", "Erro ao ler arquivo", e);
                            });
                        }
                    }).start();
                    
                } else if (requestCode == CREATE_FILE) {
                    // Exportação de arquivo
                    progressDialog.setMessage("Exportando conteúdo...");
                    progressDialog.show();
                    
                    new Thread(() -> {
                        try {
                            exportFile(uri);
                            
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(this, "Conteúdo exportado com sucesso!", Toast.LENGTH_LONG).show();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(this, "Erro ao exportar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e("EXPORT", "Erro ao exportar", e);
                            });
                        }
                    }).start();
                }
            } catch (SecurityException e) {
                progressDialog.dismiss();
                Toast.makeText(this, "Permissão negada para acessar o arquivo", Toast.LENGTH_LONG).show();
                Log.e("FILE_ACCESS", "Erro de permissão", e);
            } catch (Exception e) {
                progressDialog.dismiss();
                Toast.makeText(this, "Erro inesperado: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("FILE_IMPORT", "Erro geral", e);
            }
        } else if (resultCode == RESULT_CANCELED) {
            // Usuário cancelou a seleção de arquivo
            Toast.makeText(this, "Importação cancelada", Toast.LENGTH_SHORT).show();
        }
    }

    private String readTextFileWithEncodingDetection(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, length);
        }
        byte[] fileContentBytes = byteArrayOutputStream.toByteArray();
        inputStream.close();

        try {
            String content = new String(fileContentBytes, StandardCharsets.UTF_8);
            if (!hasInvalidUTF8Characters(content)) {
                return content;
            }
        } catch (Exception e) {
        }

        try {
            return new String(fileContentBytes, "Windows-1252");
        } catch (Exception e) {
        }

        return new String(fileContentBytes, StandardCharsets.ISO_8859_1);
    }

    private boolean hasInvalidUTF8Characters(String content) {
        return content.contains("�");
    }

    private String extractTextFromPdf(Uri uri) {
        InputStream inputStream = null;
        PDDocument document = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                throw new IOException("Não foi possível abrir o arquivo PDF");
            }
    
            // Usar um buffer maior para PDFs complexos
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, 8192);
            
            // Carrega o documento com tratamento de memória
            document = PDDocument.load(bufferedInputStream);
    
            // Verifica se o PDF está criptografado
            if (document.isEncrypted()) {
                try {
                    // Tentar abrir sem senha (para alguns PDFs marcados como criptografados mas sem senha real)
                    document.setAllSecurityToBeRemoved(true);
                } catch (Exception e) {
                    throw new IOException("PDF criptografado não é suportado");
                }
            }
    
            // Configurar o extrator de texto
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n"); // Manter consistência com o tratamento de linhas
            
            // Limitar o número de páginas para evitar problemas com PDFs muito grandes
            if (document.getNumberOfPages() > 100) {
                stripper.setEndPage(100); // Limitar às primeiras 100 páginas
            }
    
            String text = stripper.getText(document);
            
            // Fechar recursos antes de retornar
            document.close();
            inputStream.close();
            
            return text;
    
        } catch (OutOfMemoryError e) {
            runOnUiThread(() -> Toast.makeText(
                this, 
                "PDF muito grande - memória insuficiente", 
                Toast.LENGTH_LONG
            ).show());
            return null;
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(
                this, 
                "Erro ao processar PDF: " + e.getMessage(), 
                Toast.LENGTH_LONG
            ).show());
            return null;
        } finally {
            try {
                if (document != null) {
                    document.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                // Ignora erros ao fechar
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
                content.append(item.getOriginalLine()).append("\n");
            }
        } else {
            for (QAItem item : items) {
                content.append(item.getOriginalLine() != null ? item.getOriginalLine() : item.getText()).append("\n");
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
            for (QAItem item : originalItems) {
                if (item.isQA()) {
                    content.append(item.getOriginalLine()).append("\n");
                } else {
                    content.append(item.getText()).append("\n");
                }
            }
        } else {
            for (QAItem item : originalItems) {
                content.append(item.getOriginalLine() != null ? 
                              item.getOriginalLine() : item.getText()).append("\n");
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
            String originalLines = lines[i] + "\n" + lines[i+1];
            items.add(new QAItem(question, answer, originalLines));
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
            if (isQAMode) {
                answerTextView.setVisibility(View.VISIBLE);
            }
        }
        
        updateDisplay();
    }
    
    private void goToPrevSearchResult() {
        if (searchResults.isEmpty()) return;
        
        currentSearchIndex = (currentSearchIndex - 1 + searchResults.size()) % searchResults.size();
        currentIndex = searchResults.get(currentSearchIndex);
        updateDisplay();
        updateSearchInfo();
        if (isQAMode) {
            answerTextView.setVisibility(View.VISIBLE);
        }
    }
    
    private void goToNextSearchResult() {
        if (searchResults.isEmpty()) return;
        
        currentSearchIndex = (currentSearchIndex + 1) % searchResults.size();
        currentIndex = searchResults.get(currentSearchIndex);
        updateDisplay();
        updateSearchInfo();
        if (isQAMode) {
            answerTextView.setVisibility(View.VISIBLE);
        }
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
                    loadedOriginalItems.add(new QAItem(parts[0].trim(), parts[1].trim(), line));
                } else {
                    loadedOriginalItems.add(new QAItem(line.trim(), line));
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
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
}
