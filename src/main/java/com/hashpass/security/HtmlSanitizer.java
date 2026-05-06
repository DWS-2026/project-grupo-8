package com.hashpass.security;

import java.net.InetAddress;
import java.net.Inet6Address;
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
            // disallow embedded credentials (user:pass@host)
            if (uri.getUserInfo() != null) {
                return null;
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            String asciiHost = IDN.toASCII(host.trim().toLowerCase(Locale.ROOT));
            if (isBlockedHost(asciiHost)) {
                return null;
            }
            return sanitizedUrl;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isBlockedHost(String host) {
        String normalizedHost = host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
        if (normalizedHost.isBlank()) {
            return true;
        }

        // quick textual checks
        if ("localhost".equals(normalizedHost)
                || normalizedHost.endsWith(".localhost")
                || normalizedHost.equals("metadata.google.internal")) {
            return true;
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(normalizedHost);
            for (InetAddress address : addresses) {
                // general Java checks (covers many cases)
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    return true;
                }

                byte[] addr = address.getAddress();
                if (addr == null) {
                    return true;
                }

                // IPv4 explicit checks (including CGNAT 100.64.0.0/10)
                if (addr.length == 4) {
                    int b0 = addr[0] & 0xFF;
                    int b1 = addr[1] & 0xFF;

                    if (b0 == 10) return true; // 10.0.0.0/8
                    if (b0 == 127) return true; // 127.0.0.0/8
                    if (b0 == 169 && b1 == 254) return true; // 169.254.0.0/16
                    if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return true; // 172.16.0.0/12
                    if (b0 == 192 && b1 == 168) return true; // 192.168.0.0/16
                    if (b0 == 100 && (b1 >= 64 && b1 <= 127)) return true; // 100.64.0.0/10 (CGNAT)
                }

                // IPv6 checks
                if (addr.length == 16) {
                    int first = addr[0] & 0xFF;

                    // unique local fc00::/7 (fc or fd)
                    if ((first & 0xFE) == 0xFC) return true;

                    // link-local fe80::/10 (some JVMs covered by isLinkLocalAddress above)
                    if (first == 0xFE) {
                        int second = addr[1] & 0xFF;
                        if ((second & 0xC0) == 0x80) return true; // fe80::/10
                    }

                    // IPv4-mapped IPv6 ::ffff:a.b.c.d -> check last 4 bytes
                    boolean isIpv4Mapped = (addr[0] == 0 && addr[1] == 0 && addr[2] == 0 && addr[3] == 0
                            && addr[4] == 0 && addr[5] == 0 && addr[6] == 0 && addr[7] == 0
                            && addr[8] == 0 && addr[9] == 0 && addr[10] == (byte)0xFF && addr[11] == (byte)0xFF);
                    if (isIpv4Mapped) {
                        int b0 = addr[12] & 0xFF;
                        int b1 = addr[13] & 0xFF;
                        if (b0 == 10) return true;
                        if (b0 == 127) return true;
                        if (b0 == 169 && b1 == 254) return true;
                        if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return true;
                        if (b0 == 192 && b1 == 168) return true;
                        if (b0 == 100 && (b1 >= 64 && b1 <= 127)) return true;
                    }
                }
            }
        } catch (UnknownHostException ex) {
            // If host cannot be resolved, be conservative and block
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
