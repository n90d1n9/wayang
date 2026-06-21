package tech.kayys.wayang.tui;

import org.jline.utils.AttributedString;
import java.util.ArrayList;
import java.util.List;

public class Container implements Component {
    private final List<Component> children = new ArrayList<>();
    
    public void add(Component component) {
        children.add(component);
    }
    
    @Override
    public List<AttributedString> render(int width, int height) {
        List<AttributedString> lines = new ArrayList<>();
        int h = height / Math.max(1, children.size());
        for (Component child : children) {
            lines.addAll(child.render(width, h));
        }
        return lines;
    }
}
