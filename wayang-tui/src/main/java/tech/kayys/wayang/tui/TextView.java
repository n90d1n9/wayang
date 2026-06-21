package tech.kayys.wayang.tui;

import org.jline.utils.AttributedString;
import java.util.ArrayList;
import java.util.List;

public class TextView implements Component {
    private String text;
    
    public TextView(String text) {
        this.text = text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    @Override
    public List<AttributedString> render(int width, int height) {
        List<AttributedString> lines = new ArrayList<>();
        String[] parts = text.split("\n");
        for (int i = 0; i < Math.min(height, parts.length); i++) {
            String line = parts[i];
            if (line.length() > width) {
                line = line.substring(0, width);
            }
            lines.add(new AttributedString(line));
        }
        while (lines.size() < height) {
            lines.add(new AttributedString(""));
        }
        return lines;
    }
}
