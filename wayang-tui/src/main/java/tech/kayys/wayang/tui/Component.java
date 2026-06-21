package tech.kayys.wayang.tui;

import org.jline.utils.AttributedString;
import java.util.List;

public interface Component {
    List<AttributedString> render(int width, int height);
}
