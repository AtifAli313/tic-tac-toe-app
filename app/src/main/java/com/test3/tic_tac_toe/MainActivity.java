package com.test3.tic_tac_toe;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final Button[] buttons = new Button[9];
    private boolean xTurn = true;
    private int roundCount = 0;
    private TextView statusText, p1ScoreText, p2ScoreText, p1Label, p2Label;
    private LinearLayout p1Container, p2Container;
    private boolean isAiMode = true;
    private String player1Name = "Architect";
    private String player2Name = "Nexus AI";
    private int p1Score = 0, p2Score = 0;
    private volatile boolean isAiCalculating = false;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final int[][] WIN_PATHS = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        p1ScoreText = findViewById(R.id.p1Score);
        p2ScoreText = findViewById(R.id.p2Score);
        p1Label = findViewById(R.id.p1Label);
        p2Label = findViewById(R.id.p2Label);
        p1Container = findViewById(R.id.player1Container);
        p2Container = findViewById(R.id.player2Container);

        // Branding Animation
        View devBranding = findViewById(R.id.devBranding);
        if (devBranding != null) {
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 0.7f);
            fadeIn.setDuration(1500);
            fadeIn.setFillAfter(true);
            devBranding.startAnimation(fadeIn);
        }

        for (int i = 0; i < 9; i++) {
            String btnID = "btn_" + i;
            int resID = getResources().getIdentifier(btnID, "id", getPackageName());
            buttons[i] = findViewById(resID);
            buttons[i].setOnClickListener(this);
        }

        findViewById(R.id.resetBtn).setOnClickListener(v -> resetGame());
        findViewById(R.id.modeBtn).setOnClickListener(v -> showStartDialog());

        // Show dialog on start
        mainHandler.postDelayed(this::showStartDialog, 500);

        updateScoreDisplay();
        updateTurnUI();
    }

    private void showStartDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_start, null);
        
        EditText editP1 = view.findViewById(R.id.editPlayer1Name);
        EditText editP2 = view.findViewById(R.id.editPlayer2Name);
        RadioGroup modeGroup = view.findViewById(R.id.modeGroup);
        RadioButton radioAi = view.findViewById(R.id.radioAi);
        View labelP2 = view.findViewById(R.id.labelPlayer2);

        editP1.setText(player1Name);
        editP2.setText(player2Name.equals("Nexus AI") ? "Guest" : player2Name);

        modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioAi) {
                editP2.setVisibility(View.GONE);
                labelP2.setVisibility(View.GONE);
            } else {
                editP2.setVisibility(View.VISIBLE);
                labelP2.setVisibility(View.VISIBLE);
            }
        });

        // Initialize visibility
        if (radioAi.isChecked()) {
            editP2.setVisibility(View.GONE);
            labelP2.setVisibility(View.GONE);
        }

        builder.setView(view)
                .setCancelable(false)
                .setPositiveButton("ENTER ARENA", (d, w) -> {
                    player1Name = editP1.getText().toString().trim().isEmpty() ? "Architect" : editP1.getText().toString();
                    isAiMode = radioAi.isChecked();
                    
                    if (isAiMode) {
                        player2Name = "Nexus AI";
                    } else {
                        player2Name = editP2.getText().toString().trim().isEmpty() ? "Challenger" : editP2.getText().toString();
                    }
                    
                    resetScores();
                    resetGame();
                }).show();
    }

    private void resetScores() {
        p1Score = 0;
        p2Score = 0;
        updateScoreDisplay();
    }

    @Override
    public void onClick(View v) {
        if (isAiCalculating) return;

        Button b = (Button) v;
        if (!b.getText().toString().isEmpty() || hasWinner()) return;

        makeMove(b, xTurn ? "X" : "O");

        if (!hasWinner() && roundCount < 9) {
            if (isAiMode && !xTurn) {
                isAiCalculating = true;
                statusText.setText(player2Name + " is calculating...");
                mainHandler.postDelayed(this::aiMove, 600);
            }
        }
    }

    private void makeMove(Button b, String p) {
        b.setText(p);
        if (p.equals("X")) {
            b.setTextColor(ContextCompat.getColor(this, R.color.primary));
        } else {
            b.setTextColor(Color.WHITE);
        }
        roundCount++;

        int[] winLine = getWinningPath(getBoardStateAsInts());
        if (winLine != null) {
            highlightWin(winLine);
            if (p.equals("X")) {
                p1Score++;
                statusText.setText(player1Name + " Wins!");
            } else {
                p2Score++;
                statusText.setText(player2Name + " Wins!");
            }
            updateScoreDisplay();
        } else if (roundCount == 9) {
            statusText.setText("Celestial Draw!");
        } else {
            xTurn = !xTurn;
            updateTurnUI();
        }
    }

    private boolean hasWinner() {
        return getWinningPath(getBoardStateAsInts()) != null;
    }

    private int[] getBoardStateAsInts() {
        int[] board = new int[9]; // 0: empty, 1: X, 2: O
        for (int i = 0; i < 9; i++) {
            String text = buttons[i].getText().toString();
            if (text.equals("X")) board[i] = 1;
            else if (text.equals("O")) board[i] = 2;
            else board[i] = 0;
        }
        return board;
    }

    private void updateTurnUI() {
        if (xTurn) {
            p1Container.setBackgroundResource(R.drawable.active_player_bg);
            p1Container.setAlpha(1.0f);
            p2Container.setBackgroundResource(R.drawable.inactive_player_bg);
            p2Container.setAlpha(0.6f);
            statusText.setText(player1Name + "'s Turn");
        } else {
            p2Container.setBackgroundResource(R.drawable.active_player_bg);
            p2Container.setAlpha(1.0f);
            p1Container.setBackgroundResource(R.drawable.inactive_player_bg);
            p1Container.setAlpha(0.6f);
            statusText.setText(player2Name + "'s Turn");
        }
    }

    private void updateScoreDisplay() {
        p1ScoreText.setText(String.valueOf(p1Score));
        p2ScoreText.setText(String.valueOf(p2Score));
        p1Label.setText(player1Name.toUpperCase() + " (X)");
        p2Label.setText(player2Name.toUpperCase() + " (O)");
    }

    private void aiMove() {
        if (roundCount >= 9 || hasWinner()) {
            isAiCalculating = false;
            return;
        }

        final int[] board = getBoardStateAsInts();
        
        executorService.execute(() -> {
            int bestScore = Integer.MIN_VALUE;
            int move = -1;

            for (int i = 0; i < 9; i++) {
                if (board[i] == 0) {
                    board[i] = 2; // AI is O
                    int score = minimax(board, 0, false);
                    board[i] = 0;
                    if (score > bestScore) {
                        bestScore = score;
                        move = i;
                    }
                }
            }

            final int finalMove = move;
            mainHandler.post(() -> {
                isAiCalculating = false;
                if (finalMove != -1 && !hasWinner()) {
                    makeMove(buttons[finalMove], "O");
                }
            });
        });
    }

    private int minimax(int[] board, int depth, boolean isMaximizing) {
        int winner = checkInternalWinner(board);
        if (winner != 0) {
            return winner == 2 ? 10 - depth : depth - 10;
        }
        if (isBoardFull(board)) return 0;

        if (isMaximizing) {
            int bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < 9; i++) {
                if (board[i] == 0) {
                    board[i] = 2;
                    bestScore = Math.max(bestScore, minimax(board, depth + 1, false));
                    board[i] = 0;
                }
            }
            return bestScore;
        } else {
            int bestScore = Integer.MAX_VALUE;
            for (int i = 0; i < 9; i++) {
                if (board[i] == 0) {
                    board[i] = 1;
                    bestScore = Math.min(bestScore, minimax(board, depth + 1, true));
                    board[i] = 0;
                }
            }
            return bestScore;
        }
    }

    private boolean isBoardFull(int[] board) {
        for (int i : board) if (i == 0) return false;
        return true;
    }

    private int checkInternalWinner(int[] board) {
        for (int[] path : WIN_PATHS) {
            if (board[path[0]] != 0 && board[path[0]] == board[path[1]] && board[path[0]] == board[path[2]]) {
                return board[path[0]];
            }
        }
        return 0;
    }

    private int[] getWinningPath(int[] board) {
        for (int[] path : WIN_PATHS) {
            if (board[path[0]] != 0 && board[path[0]] == board[path[1]] && board[path[0]] == board[path[2]]) {
                return path;
            }
        }
        return null;
    }

    private void highlightWin(int[] path) {
        int color = ContextCompat.getColor(this, R.color.win_highlight);
        for (int index : path) {
            buttons[index].setBackgroundColor(color);
        }
    }

    private void resetGame() {
        roundCount = 0;
        xTurn = true;
        isAiCalculating = false;
        for (Button b : buttons) {
            b.setText("");
            b.setBackgroundResource(R.drawable.glass_button);
        }
        updateTurnUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }
}
