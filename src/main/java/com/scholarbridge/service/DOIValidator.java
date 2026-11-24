package com.scholarbridge.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DOIValidator {
    // DOI format: 10.<registrant 4+ chars>/<suffix>
    private static final Pattern DOI_PATTERN =
            Pattern.compile("10\\.\\d{4,9}/[-._;()/:A-Z0-9]+", Pattern.CASE_INSENSITIVE);

    public boolean isValidFormat(String doi) {
        if (doi == null || doi.isBlank()) return false;
        String normalized = normalize(doi);
        return DOI_PATTERN.matcher(normalized).matches();
    }

    public String extract(String text) {
        if (text == null) return null;
        Pattern doi = Pattern.compile(
                "(?:doi\\s*(?:[:=]|https?://(?:dx\\.)?doi\\.org/)\\s*)(10\\.\\d{4,9}/[-._;()/:A-Z0-9]+)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = doi.matcher(text);
        return matcher.find() ? normalize(matcher.group(1)) : null;
    }

    public String normalize(String doi) {
        if (doi == null) return null;
        String value = doi.trim();
        value = value.replaceAll("(?i)^https?://(?:dx\\.)?doi\\.org/", "");
        value = value.replaceAll("(?i)^doi\\s*[:=]?\\s*", "");
        return value;
    }
}
