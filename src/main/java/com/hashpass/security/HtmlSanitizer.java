package com.hashpass.security;

import java.net.InetAddress;
import java.net.IDN;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;

/**
 * Centralized input sanitization service.
 * Keeps plain-text, email and rich-text handling in one place.
 */
@Service
public class HtmlSanitizer {

    private static final PolicyFactory REVIEW_HTML_POLICY = new HtmlPolicyBuilder()
            .allowElements("p", "br", "strong", "em", "u", "blockquote", "code", "pre", "ul", "ol", "li", "a")
            .allowAttributes("href").onElements("a")
            .allowStandardUrlProtocols()
            .requireRelNofollowOnLinks()
            .toFactory();

    public String sanitizePlainText(String input) {
        if (input == null) {
            return null;
        }
        return normalizeWhitespace(Jsoup.parse(input).text());
    }

    public String sanitizeOptionalPlainText(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        return sanitizePlainText(input);
    }

    public String normalizeEmail(String input) {
        String normalizedEmail = sanitizePlainText(input);
        if (normalizedEmail == null) {
            return null;
        }
        return normalizedEmail.toLowerCase(Locale.ROOT);
    }

    public String sanitizePhoneNumber(String input) {
        String sanitizedPhone = sanitizeOptionalPlainText(input);
        if (sanitizedPhone == null) {
            return null;
        }
        if (!sanitizedPhone.matches("[0-9+()\\s-]{6,32}")) {
            return null;
        }
        return sanitizedPhone;
    }

    public String sanitizeRichText(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }
        return normalizeWhitespace(REVIEW_HTML_POLICY.sanitize(rawHtml));
    }

    public String sanitizeUrl(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String sanitizedUrl = sanitizePlainText(input);
        if (sanitizedUrl == null || sanitizedUrl.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(sanitizedUrl);
            String scheme = uri.getScheme();
            if (scheme != null
                    && !"http".equalsIgnoreCase(scheme)
                    && !"https".equalsIgnoreCase(scheme)) {
                return null;
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }

            if (isBlockedHost(host)) {
                return null;
            }
            return sanitizedUrl;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isBlockedHost(String host) {
        String normalizedHost = IDN.toASCII(host.trim().toLowerCase(Locale.ROOT));
        if (normalizedHost.isBlank()) {
            return true;
        }

        if ("localhost".equals(normalizedHost)
                || normalizedHost.endsWith(".localhost")
                || normalizedHost.equals("metadata.google.internal")) {
            return true;
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(normalizedHost);
            for (InetAddress address : addresses) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()) {
                    return true;
                }
            }
        } catch (UnknownHostException ex) {
            return true;
        }

        return false;
    }

    public String extractPlainText(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }
        return normalizeWhitespace(Jsoup.parse(rawHtml).text());
    }

    private String normalizeWhitespace(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("\\s+", " ").trim();
    }
}
