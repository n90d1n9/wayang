package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangClient;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandQuery;
import tech.kayys.wayang.tui.Component;
import tech.kayys.wayang.tui.Container;
import tech.kayys.wayang.tui.Border;
import tech.kayys.wayang.tui.TextView;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.util.List;

final class WayangGollekTuiApp {

    private final WayangClient client;
    private final WayangGollekTuiView view;

    WayangGollekTuiApp(WayangClient client) {
        this.client = client;
        this.view = new WayangGollekTuiView();
    }

    public void run() throws IOException {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            terminal.puts(InfoCmp.Capability.enter_ca_mode);
            terminal.puts(InfoCmp.Capability.clear_screen);
            
            int width = terminal.getWidth();
            int height = terminal.getHeight();
            
            var snapshot = client.contexts().workspace(".", 200, false);
            Component root = view.render(client.commands().workbench(WorkbenchCommandQuery.all()), snapshot);
            
            List<AttributedString> lines = root.render(width, height);
            for (AttributedString line : lines) {
                terminal.writer().println(line.toAnsi(terminal));
            }
            terminal.flush();
            
            terminal.writer().println("Press any key to exit...");
            terminal.flush();
            terminal.reader().read();
            
            terminal.puts(InfoCmp.Capability.exit_ca_mode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
