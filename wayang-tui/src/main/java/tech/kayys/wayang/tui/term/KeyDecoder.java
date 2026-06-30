package tech.kayys.wayang.tui.term;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads raw bytes from an InputStream (stdin in raw mode) and decodes
 * them into {@link Key} events: plain UTF-8 codepoints, control chars,
 * and common ANSI/VT100 escape sequences (arrows, home/end, delete,
 * page up/down, and a few terminal-specific variants for shift/alt+enter).
 */
public final class KeyDecoder {

    private final InputStream in;

    public KeyDecoder(InputStream in) {
        this.in = in;
    }

    /** Blocks until a full key (possibly a multi-byte escape sequence) is available. */
    public Key readKey() throws IOException {
        int b = in.read();
        if (b == -1) return Key.of(Key.Kind.CTRL_D); // EOF, e.g. piped input closed

        switch (b) {
            case 3: return Key.of(Key.Kind.CTRL_C);
            case 4: return Key.of(Key.Kind.CTRL_D);
            case 12: return Key.of(Key.Kind.CTRL_L);
            case 18: return Key.of(Key.Kind.CTRL_R);
            case 1: return Key.of(Key.Kind.CTRL_A);
            case 5: return Key.of(Key.Kind.CTRL_E);
            case 11: return Key.of(Key.Kind.CTRL_K);
            case 21: return Key.of(Key.Kind.CTRL_U);
            case 23: return Key.of(Key.Kind.CTRL_W);
            case 9: return Key.of(Key.Kind.TAB);
            case 10: return Key.of(Key.Kind.NEWLINE); // raw LF, e.g. pasted multi-line text
            case 13: return Key.of(Key.Kind.ENTER);
            case 127, 8: return Key.of(Key.Kind.BACKSPACE);
            case 27: return readEscapeSequence();
            default:
                return decodeUtf8(b);
        }
    }

    private Key readEscapeSequence() throws IOException {
        // A lone ESC (no follow-up byte available) is the Escape key itself.
        if (!waitByteAvailable()) return Key.of(Key.Kind.ESCAPE);

        int b1 = in.read();
        if (b1 == -1) return Key.of(Key.Kind.ESCAPE);

        if (b1 == 13 || b1 == 10) return Key.of(Key.Kind.ALT_ENTER); // ESC + Enter (some terminals: Alt+Enter)

        if (b1 != '[' && b1 != 'O') {
            // Unrecognized ESC-prefixed sequence; treat as escape, ignore the rest.
            return Key.of(Key.Kind.ESCAPE);
        }

        // CSI sequence: ESC [ params... final
        StringBuilder params = new StringBuilder();
        int finalByte;
        while (true) {
            int b = in.read();
            if (b == -1) return Key.of(Key.Kind.UNKNOWN);
            if (b >= '0' && b <= '9' || b == ';') {
                params.append((char) b);
            } else {
                finalByte = b;
                break;
            }
        }

        String p = params.toString();
        return switch (finalByte) {
            case 'A' -> Key.of(Key.Kind.ARROW_UP);
            case 'B' -> Key.of(Key.Kind.ARROW_DOWN);
            case 'C' -> Key.of(Key.Kind.ARROW_RIGHT);
            case 'D' -> Key.of(Key.Kind.ARROW_LEFT);
            case 'H' -> Key.of(Key.Kind.HOME);
            case 'F' -> Key.of(Key.Kind.END);
            case '~' -> decodeTilde(p);
            case 'u' -> decodeKittyProtocol(p); // Kitty keyboard protocol: CSI <code>;<mods>u
            default -> Key.of(Key.Kind.UNKNOWN);
        };
    }

    private Key decodeTilde(String params) {
        String code = params.split(";")[0];
        return switch (code) {
            case "1", "7" -> Key.of(Key.Kind.HOME);
            case "4", "8" -> Key.of(Key.Kind.END);
            case "3" -> Key.of(Key.Kind.DELETE);
            case "5" -> Key.of(Key.Kind.PAGE_UP);
            case "6" -> Key.of(Key.Kind.PAGE_DOWN);
            default -> Key.of(Key.Kind.UNKNOWN);
        };
    }

    private Key decodeKittyProtocol(String params) {
        // CSI <unicode-codepoint>;<modifiers>u  -- modifiers bit 1 (value 2) = shift, bit 3 (value 8) = alt/meta
        String[] parts = params.split(";");
        try {
            int cp = Integer.parseInt(parts[0]);
            int mods = parts.length > 1 ? Integer.parseInt(parts[1]) - 1 : 0;
            boolean shift = (mods & 1) != 0;
            boolean alt = (mods & 2) != 0;
            if (cp == 13) {
                if (shift) return Key.of(Key.Kind.SHIFT_ENTER);
                if (alt) return Key.of(Key.Kind.ALT_ENTER);
                return Key.of(Key.Kind.ENTER);
            }
            return Key.ofChar(cp);
        } catch (NumberFormatException e) {
            return Key.of(Key.Kind.UNKNOWN);
        }
    }

    /** Best-effort non-blocking peek so a lone ESC press doesn't hang waiting for more bytes. */
    private boolean waitByteAvailable() throws IOException {
        // A short spin-wait: real escape sequences arrive as a burst from the terminal driver.
        for (int i = 0; i < 50; i++) {
            if (in.available() > 0) return true;
            try { Thread.sleep(1); } catch (InterruptedException ignored) {}
        }
        return in.available() > 0;
    }

    private Key decodeUtf8(int first) throws IOException {
        int extra;
        int cp;
        if ((first & 0x80) == 0) {
            return Key.ofChar(first);
        } else if ((first & 0xE0) == 0xC0) {
            extra = 1; cp = first & 0x1F;
        } else if ((first & 0xF0) == 0xE0) {
            extra = 2; cp = first & 0x0F;
        } else if ((first & 0xF8) == 0xF0) {
            extra = 3; cp = first & 0x07;
        } else {
            return Key.of(Key.Kind.UNKNOWN);
        }
        for (int i = 0; i < extra; i++) {
            int b = in.read();
            if (b == -1) return Key.of(Key.Kind.UNKNOWN);
            cp = (cp << 6) | (b & 0x3F);
        }
        return Key.ofChar(cp);
    }
}
