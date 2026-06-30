package tech.kayys.wayang.tui.render;

import tech.kayys.wayang.tui.term.Ansi;
import java.util.*;
import java.util.regex.*;

/**
 * Converts a useful subset of markdown (headers, bold/italic, inline code,
 * fenced code blocks, bullet/numbered lists, blockquotes) into wrapped
 * ANSI terminal output. Not a full CommonMark implementation -- just
 * enough to make LLM responses readable in a terminal, the same spirit
 * as how Claude Code / Copilot CLI render assistant text.
 */
public final class MarkdownRenderer {

    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*|__(.+?)__");
    private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)\\*(?!\\*)|(?<!_)_(?!_)(.+?)_(?!_)");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern HEADER = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern BULLET = Pattern.compile("^(\\s*)[-*]\\s+(.*)$");
    private static final Pattern NUMBERED = Pattern.compile("^(\\s*)(\\d+)[.)]\\s+(.*)$");
    private static final Pattern BLOCKQUOTE = Pattern.compile("^>\\s?(.*)$");
    private static final Pattern FENCE = Pattern.compile("^```(\\w*)\\s*$");
    private static final Pattern HRULE = Pattern.compile("^(-{3,}|\\*{3,}|_{3,})$");

    private MarkdownRenderer() {}

    /** Renders markdown text into terminal lines wrapped to `width` columns. */
    public static List<String> render(String markdown, int width) {
        List<String> out = new ArrayList<>();
        String[] rawLines = markdown.split("\n", -1);

        boolean inCode = false;
        String codeLang = "";
        List<String> codeBuf = new ArrayList<>();

        for (String raw : rawLines) {
            Matcher fenceM = FENCE.matcher(raw.stripTrailing());
            if (fenceM.matches()) {
                if (!inCode) {
                    inCode = true;
                    codeLang = fenceM.group(1);
                    codeBuf.clear();
                } else {
                    out.addAll(renderCodeBlock(codeBuf, codeLang, width));
                    inCode = false;
                }
                continue;
            }
            if (inCode) {
                codeBuf.add(raw);
                continue;
            }
            out.addAll(renderLine(raw, width));
        }
        if (inCode && !codeBuf.isEmpty()) {
            // Unclosed fence (model got cut off mid-stream) -- render what we have.
            out.addAll(renderCodeBlock(codeBuf, codeLang, width));
        }
        return out;
    }

    private static List<String> renderLine(String line, int width) {
        if (HRULE.matcher(line.strip()).matches()) {
            return List.of(Ansi.fg(Theme.BORDER) + "─".repeat(Math.max(1, width)) + Ansi.RESET);
        }

        Matcher h = HEADER.matcher(line);
        if (h.matches()) {
            int level = h.group(1).length();
            String text = applyInline(h.group(2));
            String prefix = level <= 2 ? "▎ " : "";
            String styled = Ansi.BOLD + Ansi.fg(Theme.HEADER) + prefix + text + Ansi.RESET;
            return TextWrap.wrap(styled, width);
        }

        Matcher bq = BLOCKQUOTE.matcher(line);
        if (bq.matches()) {
            String text = applyInline(bq.group(1));
            String styled = Ansi.fg(Theme.DIM) + "│ " + Ansi.RESET + Ansi.ITALIC + text + Ansi.RESET;
            return TextWrap.wrap(styled, width);
        }

        Matcher b = BULLET.matcher(line);
        if (b.matches()) {
            String indent = b.group(1);
            String text = applyInline(b.group(2));
            String styled = indent + Ansi.fg(Theme.ACCENT) + "• " + Ansi.RESET + text;
            return wrapHanging(styled, indent.length() + 2, width);
        }

        Matcher n = NUMBERED.matcher(line);
        if (n.matches()) {
            String indent = n.group(1);
            String num = n.group(2);
            String text = applyInline(n.group(3));
            String marker = num + ". ";
            String styled = indent + Ansi.fg(Theme.ACCENT) + marker + Ansi.RESET + text;
            return wrapHanging(styled, indent.length() + marker.length(), width);
        }

        if (line.isBlank()) return List.of("");

        return TextWrap.wrap(applyInline(line), width);
    }

    /** Wraps a line where continuation rows are indented to align under the first line's text. */
    private static List<String> wrapHanging(String styled, int hangIndent, int width) {
        List<String> wrapped = TextWrap.wrap(styled, width - hangIndent);
        if (wrapped.size() <= 1) return wrapped;
        List<String> result = new ArrayList<>();
        result.add(wrapped.get(0));
        String pad = " ".repeat(Math.max(0, hangIndent));
        for (int i = 1; i < wrapped.size(); i++) result.add(pad + wrapped.get(i));
        return result;
    }

    private static List<String> renderCodeBlock(List<String> codeLines, String lang, int width) {
        List<String> out = new ArrayList<>();
        String label = lang == null || lang.isBlank() ? "code" : lang;
        String top = Ansi.fg(Theme.BORDER) + "╭─ " + Ansi.dim(label) + " " +
                "─".repeat(Math.max(0, width - label.length() - 4)) + Ansi.RESET;
        out.add(top);
        for (String codeLine : codeLines) {
            String expanded = codeLine.replace("\t", "    ");
            List<String> wrapped = TextWrap.wrap(expanded, width - 2);
            for (String w : wrapped) {
                int pad = Math.max(0, (width - 2) - Ansi.visibleLength(w));
                out.add(Ansi.fg(Theme.BORDER) + "│ " + Ansi.RESET + Ansi.fg(Theme.CODE_FG) + w +
                        " ".repeat(pad) + Ansi.RESET);
            }
        }
        out.add(Ansi.fg(Theme.BORDER) + "╰" + "─".repeat(Math.max(1, width - 1)) + Ansi.RESET);
        return out;
    }

    /** Applies inline styles (bold/italic/code) to a single logical line, in markdown source order. */
    private static String applyInline(String text) {
        // Process inline code first so its contents are never re-interpreted as bold/italic markers.
        StringBuilder result = new StringBuilder();
        int last = 0;
        Matcher codeM = INLINE_CODE.matcher(text);
        while (codeM.find()) {
            result.append(applyBoldItalic(text.substring(last, codeM.start())));
            result.append(Ansi.fg(Theme.TOOL)).append(codeM.group(1)).append(Ansi.RESET);
            last = codeM.end();
        }
        result.append(applyBoldItalic(text.substring(last)));
        return result.toString();
    }

    private static String applyBoldItalic(String text) {
        Matcher boldM = BOLD.matcher(text);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (boldM.find()) {
            sb.append(applyItalic(text.substring(last, boldM.start())));
            String content = boldM.group(1) != null ? boldM.group(1) : boldM.group(2);
            sb.append(Ansi.BOLD).append(content).append(Ansi.RESET);
            last = boldM.end();
        }
        sb.append(applyItalic(text.substring(last)));
        return sb.toString();
    }

    private static String applyItalic(String text) {
        Matcher m = ITALIC.matcher(text);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            sb.append(text, last, m.start());
            String content = m.group(1) != null ? m.group(1) : m.group(2);
            sb.append(Ansi.ITALIC).append(content).append(Ansi.RESET);
            last = m.end();
        }
        sb.append(text.substring(last));
        return sb.toString();
    }
}
