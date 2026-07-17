package tech.kayys.wayang.tui.ui;

import tech.kayys.wayang.tui.term.Ansi;
import tech.kayys.wayang.tui.term.Key;
import tech.kayys.wayang.tui.term.KeyDecoder;
import tech.kayys.wayang.tui.render.Theme;

import java.io.IOException;
import java.util.List;

public class GenericPickerWidget {
    public record PickerItem(String id, String col1, String col2, String col3, String col4) {}

    private final TermOut out;
    private final KeyDecoder keys;
    private final String title;
    private final List<PickerItem> items;
    private final int termCols;
    private final int termRows;

    public GenericPickerWidget(TermOut out, KeyDecoder keys, String title, List<PickerItem> items, int termCols, int termRows) {
        this.out = out;
        this.keys = keys;
        this.title = title;
        this.items = items;
        this.termCols = termCols;
        this.termRows = termRows;
    }

    /**
     * Renders an interactive modal picker.
     * Blocks until a selection is made or canceled.
     * @return The selected item ID, or null if canceled.
     */
    public String showAndSelect() throws IOException {
        if (items.isEmpty()) {
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
                    if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
                    if (selectedIndex >= topIndex + maxRows) topIndex = selectedIndex - maxRows + 1;
                } else if (k.kind() == Key.Kind.ENTER || k.kind() == Key.Kind.NEWLINE || (k.kind() == Key.Kind.CHAR && (k.codePoint() == '\n' || k.codePoint() == '\r'))) {
                    return items.get(selectedIndex).id();
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
        // Make sure title formatting is robust
        int titleLen = title.length();
        int dashCount = Math.max(0, w - 5 - titleLen);
        out.write(boxColor + "┌─ " + titleColor + title + " " + boxColor + "─".repeat(dashCount) + "┐" + Ansi.RESET);
        
        for (int i = 0; i < maxRows; i++) {
            int idx = topIndex + i;
            out.moveTo(r + 1 + i, c);
            if (idx < items.size()) {
                PickerItem m = items.get(idx);
                boolean isSelected = (idx == selectedIndex);
                
                String prefix = isSelected ? Ansi.fg("#00ff00") + " > " + Ansi.RESET : "   ";
                String rowColor = isSelected ? Ansi.fg("#ffffff") : Ansi.fg(Theme.ASSISTANT);
                
                String sid = padRight(m.col1(), 16);
                String sname = padRight(truncate(m.col2(), 30), 32);
                String sfmt = padRight(m.col3(), 12);
                String ssize = padRight(m.col4(), 12);
                
                String text = prefix + rowColor + sid + sname + sfmt + ssize + Ansi.RESET;
                
                int visLen = 3 + 16 + 32 + 12 + 12;
                String padding = " ".repeat(Math.max(0, w - 2 - visLen));
                out.write(boxColor + "│" + text + padding + boxColor + "│" + Ansi.RESET);
            } else {
                out.write(boxColor + "│" + " ".repeat(Math.max(0, w - 2)) + "│" + Ansi.RESET);
            }
        }
        
        out.moveTo(r + 1 + maxRows, c);
        String footer = " ↑↓ select │ Enter confirm │ Esc cancel ";
        out.write(boxColor + "└" + "─".repeat(Math.max(0, w - 2 - footer.length())) + footer + "┘" + Ansi.RESET);
        
        out.flush();
    }

    private String padRight(String s, int n) {
        if (s == null) s = "";
        if (s.length() >= n) return s.substring(0, n);
        return s + " ".repeat(n - s.length());
    }

    private String truncate(String s, int n) {
        if (s == null) return "";
        if (s.length() <= n) return s;
        return s.substring(0, n - 1) + "…";
    }

    /**
     * Renders an interactive modal picker for multiple selections.
     * Blocks until a selection is made or canceled.
     * @return The selected item IDs, or null if canceled.
     */
    public List<String> showAndSelectMultiple() throws IOException {
        if (items.isEmpty()) {
            return null;
        }

        int selectedIndex = 0;
        int topIndex = 0;
        java.util.Set<Integer> checkedIndices = new java.util.HashSet<>();
        
        int modalWidth = Math.min(termCols - 4, 80);
        int maxRows = Math.min(termRows - 6, 15);
        if (maxRows < 3) maxRows = 3;
        
        int modalRow = Math.max(2, (termRows - maxRows - 2) / 2);
        int modalCol = Math.max(2, (termCols - modalWidth) / 2);

        out.hideCursor();
        out.write(Ansi.SAVE_CURSOR);

        try {
            while (true) {
                renderMultiple(modalRow, modalCol, modalWidth, maxRows, topIndex, selectedIndex, checkedIndices);
                Key k = keys.readKey();
                
                if (k.kind() == Key.Kind.ARROW_UP) {
                    selectedIndex--;
                    if (selectedIndex < 0) selectedIndex = 0;
                    if (selectedIndex < topIndex) topIndex = selectedIndex;
                } else if (k.kind() == Key.Kind.ARROW_DOWN) {
                    selectedIndex++;
                    if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
                    if (selectedIndex >= topIndex + maxRows) topIndex = selectedIndex - maxRows + 1;
                } else if (k.kind() == Key.Kind.CHAR && k.codePoint() == ' ') {
                    if (checkedIndices.contains(selectedIndex)) {
                        checkedIndices.remove(selectedIndex);
                    } else {
                        checkedIndices.add(selectedIndex);
                    }
                } else if (k.kind() == Key.Kind.ENTER || k.kind() == Key.Kind.NEWLINE || (k.kind() == Key.Kind.CHAR && (k.codePoint() == '\n' || k.codePoint() == '\r'))) {
                    if (checkedIndices.isEmpty()) {
                        return java.util.List.of(items.get(selectedIndex).id());
                    } else {
                        return checkedIndices.stream().sorted().map(i -> items.get(i).id()).toList();
                    }
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

    private void renderMultiple(int r, int c, int w, int maxRows, int topIndex, int selectedIndex, java.util.Set<Integer> checkedIndices) {
        String boxColor = Theme.DIM;
        String titleColor = Theme.ACCENT;
        
        out.moveTo(r, c);
        int titleLen = title.length() + 14; 
        int dashCount = Math.max(0, w - 5 - titleLen);
        out.write(boxColor + "┌─ " + titleColor + title + " (Multi-select) " + boxColor + "─".repeat(dashCount) + "┐" + Ansi.RESET);
        
        for (int i = 0; i < maxRows; i++) {
            int idx = topIndex + i;
            out.moveTo(r + 1 + i, c);
            if (idx < items.size()) {
                PickerItem m = items.get(idx);
                boolean isHovered = (idx == selectedIndex);
                boolean isChecked = checkedIndices.contains(idx);
                
                String prefix = isHovered ? Ansi.fg("#00ff00") + " > " + Ansi.RESET : "   ";
                String checkStr = isChecked ? "[x] " : "[ ] ";
                String rowColor = isHovered ? Ansi.fg("#ffffff") : Ansi.fg(Theme.ASSISTANT);
                
                String sid = padRight(m.col1(), 16);
                String sname = padRight(truncate(m.col2(), 26), 28);
                String sfmt = padRight(m.col3(), 12);
                String ssize = padRight(m.col4(), 12);
                
                String text = prefix + rowColor + checkStr + sid + sname + sfmt + ssize + Ansi.RESET;
                
                int visLen = 3 + 4 + 16 + 28 + 12 + 12; // 75
                String padding = " ".repeat(Math.max(0, w - 2 - visLen));
                out.write(boxColor + "│" + text + padding + boxColor + "│" + Ansi.RESET);
            } else {
                out.write(boxColor + "│" + " ".repeat(Math.max(0, w - 2)) + "│" + Ansi.RESET);
            }
        }
        
        out.moveTo(r + 1 + maxRows, c);
        String footer = " ↑↓ move │ Space toggle │ Enter confirm │ Esc cancel ";
        out.write(boxColor + "└" + "─".repeat(Math.max(0, w - 2 - footer.length())) + footer + "┘" + Ansi.RESET);
        
        out.flush();
    }
}
