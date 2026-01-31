package com.aiclient.util;

import java.util.stream.Collectors;

public class CodeUtils {

    /**
     * Extracts code from AI response and re-indents it to match the original selection's indentation.
     * 
     * @param response The AI response containing markdown code blocks
     * @param originalSelection The original text selected by the user (indentation source)
     * @return The extracted and re-indented code
     */
    public static String extractAndIndentCode(String response, String originalSelection) {
        String code = extractCode(response);
        return reindentCode(code, originalSelection);
    }

    /**
     * Extracts code from markdown block.
     */
    private static String extractCode(String response) {
        if (response == null) return "";
        
        int start = response.indexOf("```");
        if (start == -1) return response.trim();

        // Skip language identifier (e.g. ```java)
        int codeStart = response.indexOf('\n', start);
        if (codeStart == -1) return response.trim();
        codeStart++;

        int end = response.indexOf("```", codeStart);
        if (end == -1) return response.substring(codeStart).trim();

        return response.substring(codeStart, end).trim();
    }

    /**
     * Re-indents the code to match the first line of the original selection.
     */
    private static String reindentCode(String code, String originalSelection) {
        if (code == null || code.isEmpty() || originalSelection == null || originalSelection.isEmpty()) {
            return code;
        }

        // 1. Determine base indentation from original selection
        String firstLine = originalSelection.lines().findFirst().orElse("");
        String baseIndent = getLeadingWhitespace(firstLine);

        // 2. Determine if AI code is already indented (unlikely, but possible)
        // If AI code typically starts at 0 indent, we just prepend baseIndent.
        // However, we should handle multi-line blocks.
        
        return code.lines()
                .map(line -> {
                    if (line.isBlank()) return line;
                    return baseIndent + line;
                })
                .collect(Collectors.joining("\n"));
    }

    private static String getLeadingWhitespace(String line) {
        StringBuilder indent = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (Character.isWhitespace(c)) {
                indent.append(c);
            } else {
                break;
            }
        }
        return indent.toString();
    }
}
