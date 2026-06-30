package tech.kayys.wayang.tui.ui;

import tech.kayys.wayang.tui.ui.ModelManager.ModelRow;
import tech.kayys.wayang.tui.term.Ansi;
import tech.kayys.wayang.tui.term.Key;
import tech.kayys.wayang.tui.term.KeyDecoder;
import tech.kayys.wayang.tui.render.Theme;

import java.io.IOException;
import java.util.List;

public class ModelPickerWidget {
    private final TermOut out;
    private final KeyDecoder keys;
    private final List<ModelRow> models;
    private final int termCols;
    private final int termRows;

    public ModelPickerWidget(TermOut out, KeyDecoder keys, List<ModelRow> models, int termCols, int termRows) {
        this.out = out;
        this.keys = keys;
        this.models = models;
        this.termCols = termCols;
        this.termRows = termRows;
    }

    /**
     * Renders an interactive modal picker.
     * Blocks until a selection is made or canceled.
     * @return The selected model short ID, or null if canceled.
     */
    public String showAndSelect() throws IOException {
        if (models.isEmpty()) {
            return null;
        }

        int selectedIndex = 0;
        int topIndex = 0;
        
        // Calculate modal dimensions
        int modalWidth = Math.min(termCols - 4, 80);
        int maxRows = Math.min(termRows - 6, 15);
        if (maxRows < 3) maxRows = 3;
        
        // Modal position
        int modalRow = Math.max(2, (termRows - maxRows - 2) / 2);
        int modalCol = Math.max(2, (termCols - modalWidth) / 2);

        out.hideCursor();
        out.write(Ansi.SAVE_CURSOR);

        try {
            while (true) {
                render(modalRow, modalCol, modalWidth, maxRows, topIndex, selectedIndex);
                Key k = keys.readKey();
                
                if (k.kind() == Key.Kind.ARROW_UP) {
                    selectedIndex--;
                    if (selectedIndex < 0) selectedIndex = 0;
                    if (selectedIndex < topIndex) topIndex = selectedIndex;
                } else if (k.kind() == Key.Kind.ARROW_DOWN) {
                    selectedIndex++;
                    if (selectedIndex >= models.size()) selectedIndex = models.size() - 1;
                    if (selectedIndex >= topIndex + maxRows) topIndex = selectedIndex - maxRows + 1;
                } else if (k.kind() == Key.Kind.ENTER || k.kind() == Key.Kind.NEWLINE || (k.kind() == Key.Kind.CHAR && (k.codePoint() == '\n' || k.codePoint() == '\r'))) {
                    return models.get(selectedIndex).shortId();
                } else if (k.kind() == Key.Kind.ESCAPE || (k.kind() == Key.Kind.CHAR && (k.codePoint() == 'q' || k.codePoint() == 'Q'))) {
                    return null;
                }
            }
        } finally {
            out.write(Ansi.RESTORE_CURSOR);
            out.showCursor();
            out.flush();
        }
    }

    private void render(int r, int c, int w, int maxRows, int topIndex, int selectedIndex) {
        String boxColor = Theme.DIM;
        String titleColor = Theme.ACCENT;
        
        out.moveTo(r, c);
        out.write(boxColor + "┌─ " + titleColor + "Available Models " + boxColor + "─".repeat(Math.max(0, w - 21)) + "┐" + Ansi.RESET);
        
        for (int i = 0; i < maxRows; i++) {
            int idx = topIndex + i;
            out.moveTo(r + 1 + i, c);
            if (idx < models.size()) {
                ModelRow m = models.get(idx);
                boolean isSelected = (idx == selectedIndex);
                
                String prefix = isSelected ? Ansi.fg("#00ff00") + " > " + Ansi.RESET : "   ";
                String rowColor = isSelected ? Ansi.fg("#ffffff") : Ansi.fg(Theme.ASSISTANT);
                
                String sid = padRight(m.shortId(), 8);
                String sname = padRight(truncate(m.name(), 30), 32);
                String sfmt = padRight(m.format(), 8);
                String ssize = padRight(m.sizeStr(), 8);
                
                String text = prefix + rowColor + sid + sname + sfmt + ssize + Ansi.RESET;
                
                int padding = w - 2 - Ansi.visibleLength(text);
                out.write(boxColor + "│" + text + " ".repeat(Math.max(0, padding)) + boxColor + "│" + Ansi.RESET);
            } else {
                out.write(boxColor + "│" + " ".repeat(w - 2) + "│" + Ansi.RESET);
            }
        }
        
        out.moveTo(r + maxRows + 1, c);
        String footer = " \u2191\u2193 select \u2502 Enter confirm \u2502 Esc cancel ";
        out.write(boxColor + "└" + "─".repeat(Math.max(0, w - footer.length() - 2)) + footer + "─┘" + Ansi.RESET);
        out.flush();
    }
    
    private String padRight(String s, int n) {
        if (s == null) return " ".repeat(n);
        if (s.length() >= n) return s.substring(0, n);
        return s + " ".repeat(n - s.length());
    }

    private String truncate(String s, int n) {
        if (s == null) return "";
        if (s.length() <= n) return s;
        return s.substring(0, n - 3) + "...";
    }
}
