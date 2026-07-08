package tech.kayys.wayang.tui.ui;

import tech.kayys.wayang.tui.ui.ProviderManager.ProviderRow;
import tech.kayys.wayang.tui.term.Ansi;
import tech.kayys.wayang.tui.term.Key;
import tech.kayys.wayang.tui.term.KeyDecoder;
import tech.kayys.wayang.tui.render.Theme;

import java.io.IOException;
import java.util.List;

public class ProviderPickerWidget {
    private final TermOut out;
    private final KeyDecoder keys;
    private final List<ProviderRow> providers;
    private final int termCols;
    private final int termRows;

    public ProviderPickerWidget(TermOut out, KeyDecoder keys, List<ProviderRow> providers, int termCols, int termRows) {
        this.out = out;
        this.keys = keys;
        this.providers = providers;
        this.termCols = termCols;
        this.termRows = termRows;
    }

    public String showAndSelect() throws IOException {
        List<GenericPickerWidget.PickerItem> items = providers.stream()
            .map(p -> new GenericPickerWidget.PickerItem(p.id(), p.id(), p.name(), p.version(), p.status()))
            .toList();
        
        GenericPickerWidget generic = new GenericPickerWidget(out, keys, "Available Providers", items, termCols, termRows);
        return generic.showAndSelect();
    }
}
