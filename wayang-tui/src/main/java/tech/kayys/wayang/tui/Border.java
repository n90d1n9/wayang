package tech.kayys.wayang.tui;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import java.util.ArrayList;
import java.util.List;

public class Border implements Component {
    private final Component inner;
    private final String title;
    
    public Border(Component inner, String title) {
        this.inner = inner;
        this.title = title;
    }
    
    @Override
    public List<AttributedString> render(int width, int height) {
        List<AttributedString> result = new ArrayList<>();
        
        // Top border
        String top = "┌─ " + title + " ";
        while (top.length() < width - 1) top += "─";
        top += "┐";
        result.add(new AttributedString(top));
        
        // Inner
        List<AttributedString> innerLines = inner.render(width - 2, height - 2);
        for (AttributedString line : innerLines) {
            String content = line.toAnsi(); // simplified for scaffold
            org.jline.utils.AttributedStringBuilder asb = new org.jline.utils.AttributedStringBuilder();
            asb.append("│ ");
            asb.append(line);
            asb.append(" │");
            result.add(asb.toAttributedString());
        }
        
        // Bottom border
        String bottom = "└";
        while (bottom.length() < width - 1) bottom += "─";
        bottom += "┘";
        result.add(new AttributedString(bottom));
        
        return result;
    }
}
