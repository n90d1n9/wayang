package tech.kayys.wayang.tui;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import java.util.List;

public class InputBox implements Component {
    private String prompt;
    private String input;
    
    public InputBox(String prompt, String input) {
        this.prompt = prompt;
        this.input = input;
    }
    
    @Override
    public List<AttributedString> render(int width, int height) {
        String line = prompt + " " + input;
        if (line.length() > width) {
            line = line.substring(line.length() - width);
        }
        AttributedString str = new AttributedString(line, AttributedStyle.DEFAULT.inverse());
        return List.of(str);
    }
}
