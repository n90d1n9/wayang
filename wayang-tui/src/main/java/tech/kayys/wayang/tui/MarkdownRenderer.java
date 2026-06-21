package tech.kayys.wayang.tui;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

public class MarkdownRenderer {
    public static AttributedString renderLine(String line) {
        if (line.startsWith("# ")) {
            return new AttributedString(line, AttributedStyle.BOLD.foreground(AttributedStyle.BLUE));
        } else if (line.startsWith("## ")) {
            return new AttributedString(line, AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        }
        return new AttributedString(line);
    }
}
