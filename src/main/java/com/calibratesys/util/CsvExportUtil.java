package com.calibratesys.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Utility for producing RFC-4180 compliant CSV exports with proper escaping and UTF-8 encoding.
 */
public final class CsvExportUtil {

    private CsvExportUtil() {
    }

    /**
     * Escapes a single CSV cell value according to RFC-4180 and protects against CSV formula injection.
     * Values starting with =, +, -, or @ that are not simple numbers are prefixed with a single quote (').
     */
    public static String escapeField(Object value) {
        if (value == null) {
            return "";
        }
        String str = value.toString();

        // Formula injection protection (OWASP CSV Injection mitigation)
        if (!str.isEmpty()) {
            char firstChar = str.charAt(0);
            if (firstChar == '=' || firstChar == '@') {
                str = "'" + str;
            } else if ((firstChar == '+' || firstChar == '-') && !isNumeric(str)) {
                str = "'" + str;
            }
        }

        boolean needsQuotes = str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r");
        if (str.contains("\"")) {
            str = str.replace("\"", "\"\"");
        }
        if (needsQuotes) {
            return "\"" + str + "\"";
        }
        return str;
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Formats headers and data rows into a valid UTF-8 CSV string.
     */
    public static byte[] generateCsvBytes(List<String> headers, List<List<String>> rows) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            // Write Header
            if (headers != null && !headers.isEmpty()) {
                StringBuilder headerLine = new StringBuilder();
                for (int i = 0; i < headers.size(); i++) {
                    if (i > 0) {
                        headerLine.append(",");
                    }
                    headerLine.append(escapeField(headers.get(i)));
                }
                writer.println(headerLine);
            }

            // Write Data Rows
            if (rows != null) {
                for (List<String> row : rows) {
                    StringBuilder line = new StringBuilder();
                    for (int i = 0; i < row.size(); i++) {
                        if (i > 0) {
                            line.append(",");
                        }
                        line.append(escapeField(row.get(i)));
                    }
                    writer.println(line);
                }
            }
            writer.flush();
        }
        return baos.toByteArray();
    }
}
