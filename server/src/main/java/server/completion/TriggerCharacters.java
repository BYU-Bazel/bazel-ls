package server.completion;

import java.util.Arrays;
import java.util.List;

public class TriggerCharacters {
    public static final String COLON = ":";
    public static final String SINGLE_SLASH = "/";
    public static final String DOUBLE_SLASH = "//";
    public static final String SINGLE_QUOTE = "'";
    public static final String DOUBLE_QUOTE = "\"";
    public static final String QUOTE_REGEX = "([\"'])(?:(.*?)\\1|(?:.(?!\\1))*$)";

    private TriggerCharacters() {
        super();
    }

    public static List<String> all() {
        return Arrays.asList(COLON, SINGLE_SLASH, SINGLE_QUOTE, DOUBLE_QUOTE);
    }
}
