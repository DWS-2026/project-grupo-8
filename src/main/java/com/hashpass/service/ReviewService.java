package com.hashpass.service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import com.hashpass.model.Review;
import com.hashpass.repository.ReviewRepository;

@Service
public class ReviewService {

    private static final Pattern SCRIPT_BLOCK_PATTERN = Pattern.compile("(?is)<script.*?>.*?</script>");
    private static final Pattern STYLE_BLOCK_PATTERN = Pattern.compile("(?is)<style.*?>.*?</style>");
    private static final Pattern ENCODED_DANGEROUS_TAG_PATTERN = Pattern.compile(
            "(?is)&lt;\\s*/?\\s*(script|style|img|svg|iframe|object|embed|link|meta|base|form|input|button)\\b.*?&gt;");

    private static final PolicyFactory REVIEW_HTML_POLICY = new HtmlPolicyBuilder()
        .allowElements("p", "br", "strong", "em", "u", "blockquote", "code", "pre", "ul", "ol", "li", "a")
        .allowAttributes("href").onElements("a")
        .allowStandardUrlProtocols()
        .requireRelNofollowOnLinks()
        .toFactory();

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public Optional<Review> findById(Long id) {
        return reviewRepository.findById(id);
    }

    public Review save(Review review) {
        return reviewRepository.save(review);
    }

    public void delete(Review review) {
        reviewRepository.delete(review);
    }

    public List<Review> findAllByOrderByCreatedAtDesc() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    public Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable) {
        return reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public List<Review> findTop3ByOrderByRatingDescCreatedAtDesc() {
        return reviewRepository.findTop3ByOrderByRatingDescCreatedAtDesc();
    }

    public String sanitizeRichText(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }

        String cleanedInput = SCRIPT_BLOCK_PATTERN.matcher(rawHtml).replaceAll("");
        cleanedInput = STYLE_BLOCK_PATTERN.matcher(cleanedInput).replaceAll("");
        cleanedInput = REVIEW_HTML_POLICY.sanitize(cleanedInput);
        cleanedInput = ENCODED_DANGEROUS_TAG_PATTERN.matcher(cleanedInput).replaceAll("");
        return cleanedInput.trim();
    }

    public String extractPlainText(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }

        String withoutTags = rawHtml.replaceAll("<[^>]+>", " ");
        return withoutTags.replaceAll("\\s+", " ").trim();
    }
}