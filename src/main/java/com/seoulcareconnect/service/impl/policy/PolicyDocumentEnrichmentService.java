package com.seoulcareconnect.service.impl.policy;

import com.seoulcareconnect.entity.policy.PolicySource;
import com.seoulcareconnect.entity.policy.RawCollectedItem;
import com.seoulcareconnect.entity.policy.enums.RawType;
import com.seoulcareconnect.integration.policy.ExternalPolicyDocument;
import com.seoulcareconnect.integration.policy.ExternalPolicyItem;
import com.seoulcareconnect.repository.policy.RawCollectedItemRepository;
import com.seoulcareconnect.service.ai.AiPolicyApplicationExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyDocumentEnrichmentService {

    private static final int MAX_RESULT_ITEMS = 30;
    private static final int MAX_ITEM_LENGTH = 300;
    private static final int MAX_HTML_BYTES = 2_000_000;
    private static final int MAX_RAW_TEXT_LENGTH = 200_000;

    private static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    + "AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/150.0.0.0 Safari/537.36";

    private static final String HTML_ACCEPT =
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";

    private static final List<String> DOCUMENT_SECTION_KEYWORDS = List.of(
            "제출서류", "제출 서류", "필요서류", "필요 서류",
            "구비서류", "구비 서류", "신청서류", "신청 서류",
            "첨부서류", "첨부 서류", "제출자료", "제출 자료",
            "준비서류", "준비 서류", "구비 항목", "준비사항"
    );

    private static final List<String> STOP_SECTION_KEYWORDS = List.of(
            "선정절차", "선정 절차", "평가방법", "평가 방법",
            "지원내용", "지원 내용", "신청방법", "신청 방법",
            "문의처", "접수기간", "신청기간", "사업개요",
            "유의사항", "주의사항", "기타사항", "문의",
            "교육안내", "교육 안내", "상세안내", "상세 안내"
    );

    private static final List<String> DOCUMENT_NAME_KEYWORDS = List.of(
            "신청서", "사업계획서", "동의서", "서약서", "확인서",
            "증명서", "등록증", "등본", "초본", "계약서", "견적서",
            "재무제표", "납세증명", "증빙", "제출서식", "제출양식",
            "신청양식", "별첨", "별지", "서식", "양식", "붙임",
            "신분증", "주민등록", "가족관계", "사업자등록", "통장사본",
            "재직", "소득", "건강보험", "원천징수", "임대차", "사본",
            "이력서", "자기소개서", "경력기술서", "포트폴리오",
            "개인정보", "졸업", "자격증", "추천서", "고용보험"
    );

    // 공고문·교육일정 같은 일반 첨부파일을 서류로 오인하지 않도록
    // 별첨·붙임 같은 표지만 제외한 강한 서류명 기준이다.
    private static final List<String> STRONG_DOCUMENT_NAME_KEYWORDS = List.of(
            "신청서", "사업계획서", "동의서", "서약서", "확인서",
            "증명서", "등록증", "등본", "초본", "계약서", "견적서",
            "재무제표", "납세증명", "증빙", "신분증", "주민등록",
            "가족관계", "사업자등록", "통장사본", "재직", "소득",
            "건강보험", "원천징수", "임대차", "이력서", "자기소개서",
            "경력기술서", "포트폴리오", "개인정보", "졸업", "자격증",
            "추천서", "고용보험"
    );

    private static final List<String> EXCLUDED_FILE_KEYWORDS = List.of(
            "공고문", "모집공고", "모집안내", "사업안내", "추진계획",
            "포스터", "홍보물", "매뉴얼", "운영지침", "설명자료",
            "교육일정", "프로그램일정"
    );

    private static final List<String> GENERIC_PLACEHOLDERS = List.of(
            "공식 공고에서 확인", "공고문에서 확인", "상세내용 공고문 참조",
            "상세 내용 공고문 참조", "제출서류는 공고문 참조",
            "제출 서류는 공고문 참조", "공고문 참조", "첨부파일 참조",
            "해당 기관에 문의", "해당 없음", "별도 없음", "제출서류 없음"
    );

    private static final List<String> NON_DOCUMENT_CONTENT_KEYWORDS = List.of(
            "신청폼 제출", "온라인 신청", "온라인신청", "접수 바로가기",
            "신청 바로가기", "사업안내 바로가기", "구글폼", "forms.gle",
            "제출하신 서류는", "서류 반환", "반환 등 문의", "알림톡",
            "자세한 내용은", "문의하여 주시기 바랍니다", "다운로드 제한 안내",
            "중소벤처24 증명서 및 사업신청 통화서비스 제공",
            "중소기업현황정보시스템 중소기업확인서 신청 및 발급 제공",
            "개인정보처리방침", "이용약관", "이메일무단수집거부"
    );

    private static final List<String> RELEVANT_LINK_KEYWORDS = List.of(
            "신청", "접수", "제출", "서류", "서식", "양식",
            "다운로드", "온라인", "홈페이지", "누리집", "구글폼"
    );

    private static final Pattern LEADING_BULLET = Pattern.compile(
            "^\\s*(?:[•▪■◆◇▶▷※*\\-–—]+|\\d{1,2}[.)])\\s*"
    );

    private static final Pattern FILE_EXTENSION = Pattern.compile(
            "(?i)\\.(?:hwp|hwpx|pdf|doc|docx|xls|xlsx|ppt|pptx|zip)$"
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)(?:https?://|www\\.|forms\\.gle|bit\\.ly|naver\\.me)"
    );

    @Value("${app.policy.documents.enrichment-enabled:true}")
    private boolean enrichmentEnabled;

    @Value("${app.policy.documents.fetch-official-page:true}")
    private boolean fetchOfficialPage;

    @Value("${app.policy.documents.fetch-when-api-documents-exist:true}")
    private boolean fetchWhenApiDocumentsExist;

    @Value("${app.policy.documents.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${app.policy.documents.curl-fallback-enabled:true}")
    private boolean curlFallbackEnabled;

    @Value("${app.policy.documents.curl-command:curl}")
    private String curlCommand;

    // 기본적으로 K-Startup에만 curl 대체 연결을 허용한다.
    // 여러 호스트를 허용하려면 쉼표로 구분한다.
    @Value("${app.policy.documents.curl-fallback-hosts:k-startup.go.kr}")
    private String curlFallbackHosts;

    private final RawCollectedItemRepository rawCollectedItemRepository;
    private final AiPolicyApplicationExtractionService aiExtractionService;

    public ExternalPolicyItem enrich(PolicySource source, ExternalPolicyItem item) {
        if (item == null) {
            return null;
        }

        List<String> apiDocuments = splitDocumentItems(item.getRequiredDocumentsText());

        if (!enrichmentEnabled
                || !fetchOfficialPage
                || (!apiDocuments.isEmpty() && !fetchWhenApiDocumentsExist)
                || !isSafePublicHttpUrl(item.getOfficialUrl())) {
            String merged = mergeDocuments(apiDocuments, List.of(), List.of(), List.of());
            return withEnrichedApplicationInfo(
                    item,
                    item.getApplyMethod(),
                    merged,
                    buildExternalDocuments(
                            splitDocumentItems(merged),
                            apiDocuments,
                            List.of(),
                            List.of(),
                            List.of()
                    )
            );
        }

        try {
            FetchResult fetchResult = fetchOfficialDocument(item.getOfficialUrl());
            Document document = fetchResult.document();

            List<String> officialSectionDocuments = extractDocumentSectionItems(document);
            List<AttachmentDocument> attachmentDocuments = extractAttachmentDocuments(document);
            List<String> attachmentDocumentNames = attachmentDocuments.stream()
                    .map(AttachmentDocument::name)
                    .toList();
            String officialText = extractOfficialPageText(document);

            RawCollectedItem officialSnapshot = findOrCreateOfficialSnapshot(
                    source,
                    item,
                    officialText,
                    document.outerHtml()
            ).orElse(null);

            AiPolicyApplicationExtractionService.Extraction aiExtraction =
                    resolveAiExtraction(item, officialText, officialSnapshot).orElse(null);
            List<String> aiDocuments = aiExtraction == null
                    ? List.of()
                    : aiExtraction.documentNames();

            String merged = mergeDocuments(
                    apiDocuments,
                    officialSectionDocuments,
                    aiDocuments,
                    attachmentDocumentNames
            );
            String enrichedApplicationMethod = resolveApplicationMethod(
                    item.getApplyMethod(),
                    aiExtraction == null ? null : aiExtraction.applicationMethod()
            );

            List<String> finalDocuments = splitDocumentItems(merged);

            log.debug(
                    "정책 신청정보 병합: title={}, 수집방식={}, API서류={}건, 원문서류={}건, "
                            + "AI서류={}건, 첨부서류={}건, 최종서류={}건, AI신청방법={}",
                    item.getTitle(),
                    fetchResult.method(),
                    apiDocuments.size(),
                    officialSectionDocuments.size(),
                    aiDocuments.size(),
                    attachmentDocumentNames.size(),
                    finalDocuments.size(),
                    aiExtraction != null
                            && StringUtils.hasText(aiExtraction.applicationMethod())
            );

            log.debug(
                    "원문 제출서류 추출 결과: title={}, API서류={}, 원문서류={}, AI서류={}, "
                            + "첨부서류={}, 최종서류={}",
                    item.getTitle(),
                    apiDocuments,
                    officialSectionDocuments,
                    aiDocuments,
                    attachmentDocumentNames,
                    finalDocuments
            );

            if (officialSectionDocuments.isEmpty()
                    && aiDocuments.isEmpty()
                    && attachmentDocumentNames.isEmpty()) {
                log.debug(
                        "공식 원문에서 제출서류명을 찾지 못했습니다: title={}, url={}",
                        item.getTitle(),
                        item.getOfficialUrl()
                );
            }

            return withEnrichedApplicationInfo(
                    item,
                    enrichedApplicationMethod,
                    merged,
                    buildExternalDocuments(
                            finalDocuments,
                            apiDocuments,
                            officialSectionDocuments,
                            aiExtraction == null
                                    ? List.of()
                                    : aiExtraction.requiredDocuments(),
                            attachmentDocuments
                    )
            );

        } catch (Exception exception) {
            log.warn(
                    "공식 원문 서류 보강 실패. API 서류를 유지합니다: "
                            + "title={}, url={}, cause={}, message={}",
                    item.getTitle(),
                    item.getOfficialUrl(),
                    exception.getClass().getName(),
                    exception.getMessage(),
                    exception
            );

            String merged = mergeDocuments(apiDocuments, List.of(), List.of(), List.of());
            return withEnrichedApplicationInfo(
                    item,
                    item.getApplyMethod(),
                    merged,
                    buildExternalDocuments(
                            splitDocumentItems(merged),
                            apiDocuments,
                            List.of(),
                            List.of(),
                            List.of()
                    )
            );
        }
    }

    private FetchResult fetchOfficialDocument(String url) throws Exception {
        try {
            return new FetchResult(fetchWithJsoup(url), "JSOUP");

        } catch (Exception jsoupException) {
            if (!curlFallbackEnabled
                    || !isCurlFallbackAllowedHost(url)
                    || !shouldUseCurlFallback(jsoupException)) {
                throw jsoupException;
            }

            log.info(
                    "Jsoup HTTPS 연결 실패로 curl 보안 연결을 사용해 재시도합니다: url={}, cause={}",
                    url,
                    rootCauseMessage(jsoupException)
            );

            return new FetchResult(fetchWithCurl(url), "CURL_FALLBACK");
        }
    }

    private Document fetchWithJsoup(String url) throws Exception {
        Connection.Response response = Jsoup.connect(url)
                .method(Connection.Method.GET)
                .userAgent(BROWSER_USER_AGENT)
                .header("Accept", HTML_ACCEPT)
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .header("Cache-Control", "no-cache")
                .timeout(Math.max(connectTimeoutMs, 1000))
                .maxBodySize(MAX_HTML_BYTES)
                .followRedirects(true)
                .execute();

        validateHtmlResponse(response.statusCode(), response.contentType());
        return response.parse();
    }

    private Document fetchWithCurl(String url) throws Exception {
        Path tempFile = Files.createTempFile("seoulcareconnect-policy-", ".html");

        int timeoutSeconds = Math.max(5, (int) Math.ceil(connectTimeoutMs / 1000.0));
        int totalTimeoutSeconds = Math.max(timeoutSeconds + 5, timeoutSeconds * 2);

        List<String> command = List.of(
                curlCommand,
                "--silent",
                "--show-error",
                "--fail",
                "--location",
                "--http1.1",
                "--compressed",
                "--connect-timeout", String.valueOf(timeoutSeconds),
                "--max-time", String.valueOf(totalTimeoutSeconds),
                "--max-filesize", String.valueOf(MAX_HTML_BYTES),
                "--user-agent", BROWSER_USER_AGENT,
                "--header", "Accept: " + HTML_ACCEPT,
                "--header", "Accept-Language: ko-KR,ko;q=0.9",
                "--output", tempFile.toString(),
                url
        );

        Process process = null;

        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(totalTimeoutSeconds + 5L, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("curl 공식 원문 요청 시간이 초과되었습니다.");
            }

            String processMessage = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            ).trim();

            if (process.exitValue() != 0) {
                throw new IOException(
                        "curl 공식 원문 요청 실패: exit="
                                + process.exitValue()
                                + ", message="
                                + limitText(processMessage, 500)
                );
            }

            long fileSize = Files.size(tempFile);
            if (fileSize <= 0) {
                throw new IOException("curl 공식 원문 응답이 비어 있습니다.");
            }
            if (fileSize > MAX_HTML_BYTES) {
                throw new IOException("curl 공식 원문 응답 크기가 제한을 초과했습니다.");
            }

            Document document = Jsoup.parse(tempFile.toFile(), null, url);
            if (document.body() == null) {
                throw new IOException("curl 응답을 HTML 문서로 해석하지 못했습니다.");
            }

            return document;

        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            Files.deleteIfExists(tempFile);
        }
    }

    private void validateHtmlResponse(int statusCode, String contentType) throws IOException {
        if (statusCode < 200 || statusCode >= 400) {
            throw new HttpStatusException(
                    "공식 원문 HTTP 응답 오류",
                    statusCode,
                    ""
            );
        }

        if (!StringUtils.hasText(contentType)
                || !contentType.toLowerCase(Locale.ROOT).contains("html")) {
            throw new IOException("공식 링크가 HTML 페이지가 아닙니다: " + contentType);
        }
    }

    private boolean isCurlFallbackAllowedHost(String url) {
        if (!StringUtils.hasText(curlFallbackHosts)) {
            return false;
        }

        try {
            String host = URI.create(url).getHost();
            if (!StringUtils.hasText(host)) {
                return false;
            }

            String normalizedHost = host.toLowerCase(Locale.ROOT);
            for (String configuredHost : curlFallbackHosts.split(",")) {
                String allowedHost = configuredHost.trim().toLowerCase(Locale.ROOT);
                if (!allowedHost.isBlank()
                        && (normalizedHost.equals(allowedHost)
                        || normalizedHost.endsWith("." + allowedHost))) {
                    return true;
                }
            }
            return false;

        } catch (Exception exception) {
            return false;
        }
    }

    private boolean shouldUseCurlFallback(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof SSLException) {
                return true;
            }

            if (current instanceof HttpStatusException statusException
                    && (statusException.getStatusCode() == 400
                    || statusException.getStatusCode() == 403)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private String extractOfficialPageText(Document document) {
        Document cleaned = document.clone();
        cleaned.select(
                "script,style,noscript,svg,canvas,iframe,header,nav,footer,aside"
        ).remove();

        Element body = cleaned.body();
        if (body == null) {
            return null;
        }

        Element content = cleaned.select(
                        "main,article,#contents,#content,.contents,.content,.board-view,.view"
                ).stream()
                .max(Comparator.comparingInt(element -> element.wholeText().length()))
                .orElse(body);

        StringBuilder result = new StringBuilder();
        String title = cleanInline(cleaned.title());
        if (StringUtils.hasText(title)) {
            result.append(title).append('\n');
        }

        String contentText = normalizeBlock(content.wholeText());
        if (StringUtils.hasText(contentText)) {
            result.append(contentText);
        }

        int linkCount = 0;
        for (Element anchor : cleaned.select("a[href]")) {
            String linkText = cleanInline(anchor.text());
            String href = cleanInline(firstNonBlank(
                    anchor.absUrl("href"),
                    anchor.attr("href")
            ));
            if (!isRelevantOfficialLink(linkText, href)) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append('\n');
            }
            result.append("[링크] ")
                    .append(StringUtils.hasText(linkText) ? linkText : "바로가기")
                    .append(" -> ")
                    .append(href);

            linkCount++;
            if (linkCount >= 100 || result.length() >= MAX_RAW_TEXT_LENGTH) {
                break;
            }
        }

        return truncateNullable(normalizeBlock(result.toString()), MAX_RAW_TEXT_LENGTH);
    }

    private boolean isRelevantOfficialLink(String text, String href) {
        if (!StringUtils.hasText(href)) {
            return false;
        }

        String withoutQuery = href.split("[?#]", 2)[0];
        String linkSource = (StringUtils.hasText(text) ? text : "") + " " + href;
        return FILE_EXTENSION.matcher(withoutQuery).find()
                || containsAny(linkSource, RELEVANT_LINK_KEYWORDS)
                || containsAny(linkSource, DOCUMENT_NAME_KEYWORDS);
    }

    private Optional<RawCollectedItem> findOrCreateOfficialSnapshot(
            PolicySource source,
            ExternalPolicyItem item,
            String officialText,
            String rawHtml
    ) {
        if (source == null
                || source.getSourceId() == null
                || !StringUtils.hasText(officialText)) {
            return Optional.empty();
        }

        String sourceUrl = truncateNullable(cleanInline(item.getOfficialUrl()), 1000);
        String policyIdentity = firstNonBlank(
                cleanInline(item.getExternalId()),
                cleanInline(item.getTitle()),
                ""
        );
        String contentHash = sha256(
                "WEB|"
                        + firstNonBlank(sourceUrl, "")
                        + "|"
                        + policyIdentity
                        + "|"
                        + officialText
        );

        try {
            return Optional.of(
                    rawCollectedItemRepository
                            .findFirstBySource_SourceIdAndRawTypeAndSourceUrlAndContentHashOrderByCollectedAtDesc(
                                    source.getSourceId(),
                                    RawType.WEB,
                                    sourceUrl,
                                    contentHash
                            )
                            .orElseGet(() -> {
                                RawCollectedItem raw = new RawCollectedItem();
                                raw.setSource(source);
                                raw.setRawType(RawType.WEB);
                                raw.setExternalId(
                                        truncateNullable(cleanInline(item.getExternalId()), 100)
                                );
                                raw.setSourceUrl(sourceUrl);
                                raw.setRawText(officialText);
                                raw.setRawHtml(rawHtml);
                                raw.setHttpStatus(200);
                                raw.setContentHash(contentHash);
                                return rawCollectedItemRepository.save(raw);
                            })
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "공식 페이지 원문 저장 실패. AI 추출은 캐시 없이 진행합니다: title={}, cause={}",
                    item.getTitle(),
                    exception.getClass().getSimpleName()
            );
            return Optional.empty();
        }
    }

    private Optional<AiPolicyApplicationExtractionService.Extraction> resolveAiExtraction(
            ExternalPolicyItem item,
            String officialText,
            RawCollectedItem officialSnapshot
    ) {
        if (!aiExtractionService.isEnabled() || !StringUtils.hasText(officialText)) {
            return Optional.empty();
        }

        if (officialSnapshot != null) {
            Optional<AiPolicyApplicationExtractionService.Extraction> cached =
                    aiExtractionService.readCached(
                            officialSnapshot.getRawJson(),
                            officialText
                    );
            if (cached.isPresent()) {
                log.debug(
                        "공식 페이지 AI 신청정보 캐시 사용: title={}, model={}",
                        item.getTitle(),
                        cached.get().modelName()
                );
                return cached;
            }
        }

        try {
            AiPolicyApplicationExtractionService.Extraction extraction =
                    aiExtractionService.extract(item, officialText);

            if (officialSnapshot != null) {
                officialSnapshot.setRawJson(aiExtractionService.writeCache(extraction));
                rawCollectedItemRepository.save(officialSnapshot);
            }
            return Optional.of(extraction);
        } catch (RuntimeException exception) {
            log.warn(
                    "공식 페이지 AI 신청정보 추출 실패. 규칙 기반 결과를 유지합니다: "
                            + "title={}, url={}, cause={}",
                    item.getTitle(),
                    item.getOfficialUrl(),
                    exception.getClass().getSimpleName()
            );
            return Optional.empty();
        }
    }

    private ExternalPolicyItem withEnrichedApplicationInfo(
            ExternalPolicyItem item,
            String applicationMethod,
            String mergedDocuments,
            List<ExternalPolicyDocument> documentCandidates
    ) {
        String currentApplicationMethod = cleanInline(item.getApplyMethod());
        String enrichedApplicationMethod = cleanInline(applicationMethod);
        String current = normalizeBlock(item.getRequiredDocumentsText());
        String merged = normalizeBlock(mergedDocuments);

        boolean sameApplicationMethod =
                (currentApplicationMethod == null && enrichedApplicationMethod == null)
                        || (currentApplicationMethod != null
                        && currentApplicationMethod.equals(enrichedApplicationMethod));
        boolean sameDocuments =
                (current == null && merged == null)
                        || (current != null && current.equals(merged));
        boolean sameCandidates = documentCandidates == null
                || documentCandidates.isEmpty()
                || documentCandidates.equals(item.getDocumentCandidates());

        if (sameApplicationMethod && sameDocuments && sameCandidates) {
            return item;
        }

        return item.toBuilder()
                .applyMethod(truncateNullable(enrichedApplicationMethod, 500))
                .requiredDocumentsText(merged)
                .documentCandidates(
                        documentCandidates == null ? List.of() : documentCandidates
                )
                .build();
    }

    private String resolveApplicationMethod(
            String apiApplicationMethod,
            String aiApplicationMethod
    ) {
        String current = cleanInline(apiApplicationMethod);
        String official = cleanInline(aiApplicationMethod);
        return StringUtils.hasText(official) ? official : current;
    }

    private List<String> extractDocumentSectionItems(Document document) {
        List<String> result = new ArrayList<>();
        Set<Element> visited = new LinkedHashSet<>();

        for (Element element : document.select(
                "h1,h2,h3,h4,h5,h6,th,td,dt,dd,strong,b,p,div,span,li"
        )) {
            String ownText = cleanInline(element.ownText());
            if (!isDocumentSectionHeading(ownText) || !visited.add(element)) {
                continue;
            }

            String inlineValue = removeDocumentHeadingPrefix(ownText);
            if (StringUtils.hasText(inlineValue)) {
                addSectionCandidates(result, inlineValue);
            }

            for (String sectionText : collectSectionTexts(element)) {
                addSectionCandidates(result, sectionText);
            }
        }

        return limit(result);
    }

    private List<String> collectSectionTexts(Element heading) {
        List<String> values = new ArrayList<>();
        String tag = heading.tagName();

        // K-Startup의 information_list 구조를 우선 처리한다.
        Element parent = heading.parent();
        if (parent != null && parent.hasClass("information_list")) {
            for (Element listItem : parent.select("ul.dot_list-wrap > li.dot_list")) {
                values.add(listItem.wholeText());
            }
            return values;
        }

        if (("th".equals(tag) || "td".equals(tag) || "dt".equals(tag))
                && heading.nextElementSibling() != null) {
            values.add(heading.nextElementSibling().wholeText());
            return values;
        }

        Element current = heading.nextElementSibling();
        int scanned = 0;

        while (current != null && scanned < 10) {
            String text = cleanInline(current.wholeText());
            if (isStopSectionHeading(text) || isDocumentSectionHeading(text)) {
                break;
            }
            if (StringUtils.hasText(text)) {
                values.add(current.wholeText());
            }
            current = current.nextElementSibling();
            scanned++;
        }

        if (values.isEmpty() && parent != null) {
            for (Element sibling : parent.children()) {
                if (sibling == heading) {
                    continue;
                }

                String text = cleanInline(sibling.wholeText());
                if (StringUtils.hasText(text)
                        && !isStopSectionHeading(text)
                        && !isDocumentSectionHeading(text)) {
                    values.add(sibling.wholeText());
                }
            }
        }

        return values;
    }

    private void addSectionCandidates(List<String> result, String sectionText) {
        if (!StringUtils.hasText(sectionText)) {
            return;
        }

        for (String candidate : sectionText.split(
                "(?:\\r?\\n)+|[•▪■◆◇▶▷]+|(?=\\[[^]]{1,20}])"
        )) {
            String cleaned = cleanItem(candidate);
            if (isUsefulDocumentItem(cleaned, true)) {
                addIfMissing(result, cleaned);
            }
        }
    }

    private List<AttachmentDocument> extractAttachmentDocuments(Document document) {
        List<AttachmentDocument> result = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();

        for (Element anchor : document.select(
                "a[href],a[data-download-url],a[data-file-url],a[data-url],a[data-href]"
        )) {
            String href = extractAttachmentLink(anchor);
            String text = firstNonBlank(
                    cleanInline(anchor.text()),
                    fileNameFromUrl(href)
            );

            String cleaned = cleanItem(text);
            String key = documentKey(cleaned);
            if (isUsefulDocumentItem(cleaned, false) && keys.add(key)) {
                result.add(new AttachmentDocument(cleaned, href));
            }
        }

        return result.stream().limit(MAX_RESULT_ITEMS).toList();
    }

    private String extractAttachmentLink(Element anchor) {
        for (String attribute : List.of(
                "href",
                "data-download-url",
                "data-file-url",
                "data-url",
                "data-href"
        )) {
            String rawValue = cleanInline(anchor.attr(attribute));
            if (!StringUtils.hasText(rawValue)
                    || rawValue.startsWith("#")
                    || rawValue.toLowerCase(Locale.ROOT).startsWith("javascript:")) {
                continue;
            }

            String safeLink = safeExternalLink(firstNonBlank(
                    anchor.absUrl(attribute),
                    rawValue
            ));
            if (safeLink != null) {
                return safeLink;
            }
        }
        return null;
    }

    private List<ExternalPolicyDocument> buildExternalDocuments(
            List<String> finalDocuments,
            List<String> apiDocuments,
            List<String> officialSectionDocuments,
            List<AiPolicyApplicationExtractionService.DocumentEvidence> aiDocuments,
            List<AttachmentDocument> attachments
    ) {
        List<ExternalPolicyDocument> result = new ArrayList<>();

        for (String document : finalDocuments) {
            AttachmentDocument attachment = bestAttachment(document, attachments);
            AiPolicyApplicationExtractionService.DocumentEvidence aiDocument =
                    bestAiDocument(document, aiDocuments);
            String sourceType;
            int confidence;

            if (containsDocument(officialSectionDocuments, document)) {
                sourceType = "OFFICIAL_TEXT";
                confidence = 95;
            } else if (containsDocument(apiDocuments, document)) {
                sourceType = "API";
                confidence = 90;
            } else if (aiDocument != null) {
                sourceType = "AI_GROUNDED";
                confidence = 85;
            } else {
                sourceType = "OFFICIAL_ATTACHMENT";
                confidence = attachment == null ? 70 : 100;
            }

            result.add(ExternalPolicyDocument.builder()
                    .text(document)
                    .evidence(aiDocument == null ? null : aiDocument.evidence())
                    .sourceType(sourceType)
                    .attachmentName(attachment == null ? null : attachment.name())
                    .downloadUrl(attachment == null ? null : attachment.url())
                    .confidence(confidence)
                    .build());
        }

        return result;
    }

    private AttachmentDocument bestAttachment(
            String document,
            List<AttachmentDocument> attachments
    ) {
        String key = documentKey(document);
        return attachments.stream()
                .filter(attachment -> {
                    String attachmentKey = documentKey(attachment.name());
                    return attachmentKey.equals(key)
                            || attachmentKey.contains(key)
                            || key.contains(attachmentKey);
                })
                .max(Comparator.comparingInt(
                        attachment -> documentKey(attachment.name()).length()
                ))
                .orElse(null);
    }

    private AiPolicyApplicationExtractionService.DocumentEvidence bestAiDocument(
            String document,
            List<AiPolicyApplicationExtractionService.DocumentEvidence> aiDocuments
    ) {
        String key = documentKey(document);
        return aiDocuments.stream()
                .filter(candidate -> {
                    String candidateKey = documentKey(candidate.name());
                    return candidateKey.equals(key)
                            || candidateKey.contains(key)
                            || key.contains(candidateKey);
                })
                .max(Comparator.comparingInt(
                        candidate -> documentKey(candidate.name()).length()
                ))
                .orElse(null);
    }

    private boolean containsDocument(List<String> documents, String candidate) {
        String candidateKey = documentKey(candidate);
        return documents.stream()
                .map(this::documentKey)
                .anyMatch(key -> key.equals(candidateKey)
                        || key.contains(candidateKey)
                        || candidateKey.contains(key));
    }

    private String mergeDocuments(
            List<String> apiDocuments,
            List<String> officialSectionDocuments,
            List<String> aiDocuments,
            List<String> attachmentDocuments
    ) {
        List<String> merged = new ArrayList<>();

        // 표시 순서 및 신뢰도: API 명시값 → 공식 원문 본문 → AI 근거 추출 → 공식 첨부파일명
        addAllSpecific(merged, apiDocuments);
        addAllSpecific(merged, officialSectionDocuments);
        addAllSpecific(merged, aiDocuments);
        addAllSpecific(merged, attachmentDocuments);

        return merged.isEmpty()
                ? null
                : String.join("\n", limit(merged));
    }

    private void addAllSpecific(List<String> merged, List<String> candidates) {
        for (String candidate : candidates) {
            String cleaned = cleanItem(candidate);
            if (!isUsefulDocumentItem(cleaned, true)) {
                continue;
            }
            addOrReplaceWithMoreSpecific(merged, cleaned);
        }
    }

    private void addOrReplaceWithMoreSpecific(List<String> merged, String candidate) {
        String candidateKey = documentKey(candidate);
        if (!StringUtils.hasText(candidateKey)) {
            return;
        }

        for (int index = 0; index < merged.size(); index++) {
            String existing = merged.get(index);
            String existingKey = documentKey(existing);

            if (candidateKey.equals(existingKey)) {
                return;
            }

            if (candidateKey.contains(existingKey)
                    && candidateKey.length() >= existingKey.length() + 4) {
                merged.set(index, candidate);
                return;
            }

            if (existingKey.contains(candidateKey)) {
                return;
            }
        }

        merged.add(candidate);
    }

    private List<String> splitDocumentItems(String text) {
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return result;
        }

        for (String item : text.split("(?:\\r?\\n)+|[•▪■◆◇▶▷]+|;")) {
            String cleaned = cleanItem(item);
            if (isUsefulDocumentItem(cleaned, true)) {
                addIfMissing(result, cleaned);
            }
        }
        return result;
    }

    private boolean isUsefulDocumentItem(String value, boolean insideDocumentSection) {
        if (!StringUtils.hasText(value)
                || value.length() > MAX_ITEM_LENGTH
                || containsAny(value, GENERIC_PLACEHOLDERS)
                || containsAny(value, NON_DOCUMENT_CONTENT_KEYWORDS)
                || isDocumentSectionHeading(value)
                || isStopSectionHeading(value)) {
            return false;
        }

        if (URL_PATTERN.matcher(value).find()
                && !containsAny(value, DOCUMENT_NAME_KEYWORDS)) {
            return false;
        }

        if (containsAny(value, EXCLUDED_FILE_KEYWORDS)
                && !containsAny(value, STRONG_DOCUMENT_NAME_KEYWORDS)) {
            return false;
        }

        if (containsAny(value, DOCUMENT_NAME_KEYWORDS)) {
            return true;
        }

        if (FILE_EXTENSION.matcher(value).find()) {
            return insideDocumentSection;
        }

        return insideDocumentSection
                && (value.contains("서류") || value.contains("자료"))
                && looksLikeListedItem(value);
    }

    private boolean looksLikeListedItem(String value) {
        return value.startsWith("[")
                || value.matches("^\\d{1,2}[.)].+")
                || value.length() <= 120;
    }

    private boolean isDocumentSectionHeading(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        String compactValue = compact(value);
        return DOCUMENT_SECTION_KEYWORDS.stream()
                .map(this::compact)
                .anyMatch(keyword -> compactValue.equals(keyword)
                        || compactValue.startsWith(keyword + ":")
                        || compactValue.startsWith(keyword + "：")
                        || (compactValue.startsWith(keyword)
                        && compactValue.length() <= keyword.length() + 180));
    }

    private String removeDocumentHeadingPrefix(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String cleaned = value.trim();
        for (String keyword : DOCUMENT_SECTION_KEYWORDS) {
            if (compact(cleaned).startsWith(compact(keyword))) {
                cleaned = cleaned.replaceFirst(
                        "^\\s*" + Pattern.quote(keyword) + "\\s*[:：-]?\\s*",
                        ""
                );
                break;
            }
        }
        return cleanInline(cleaned);
    }

    private boolean isStopSectionHeading(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        String compactValue = compact(value);
        return STOP_SECTION_KEYWORDS.stream()
                .map(this::compact)
                .anyMatch(keyword -> compactValue.equals(keyword)
                        || compactValue.startsWith(keyword + ":")
                        || compactValue.startsWith(keyword + "："));
    }

    private void addIfMissing(List<String> values, String candidate) {
        String key = documentKey(candidate);
        boolean exists = values.stream()
                .map(this::documentKey)
                .anyMatch(key::equals);

        if (!exists) {
            values.add(candidate);
        }
    }

    private List<String> limit(List<String> values) {
        return values.stream().limit(MAX_RESULT_ITEMS).toList();
    }

    private String cleanItem(String value) {
        String cleaned = cleanInline(value);
        if (cleaned == null) {
            return null;
        }

        cleaned = LEADING_BULLET.matcher(cleaned).replaceFirst("").trim();
        cleaned = cleaned.replaceFirst(
                "^(?:제출서류|제출 서류|필요서류|필요 서류|구비서류|구비 서류)\\s*[:：-]?\\s*",
                ""
        );
        return cleanInline(cleaned);
    }

    private String documentKey(String value) {
        String cleaned = cleanItem(value);
        if (cleaned == null) {
            return "";
        }

        cleaned = FILE_EXTENSION.matcher(cleaned).replaceFirst("");
        cleaned = cleaned.replaceAll("\\[[^]]{1,20}]", "");

        return cleaned
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^0-9a-z가-힣]", "");
    }

    private String fileNameFromUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }

        String withoutQuery = url.split("[?#]", 2)[0];
        int slash = withoutQuery.lastIndexOf('/');
        String fileName = slash >= 0
                ? withoutQuery.substring(slash + 1)
                : withoutQuery;

        return fileName.isBlank() ? null : fileName;
    }

    private String safeExternalLink(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            URI uri = URI.create(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    && !"http".equalsIgnoreCase(uri.getScheme())) {
                return null;
            }
            return StringUtils.hasText(uri.getHost())
                    ? truncateNullable(uri.toString(), 1000)
                    : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean isSafePublicHttpUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    || !StringUtils.hasText(host)) {
                return false;
            }

            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()) {
                    return false;
                }
            }
            return true;

        } catch (Exception exception) {
            return false;
        }
    }

    private boolean containsAny(String value, List<String> keywords) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        String compactValue = compact(value);
        return keywords.stream()
                .map(this::compact)
                .anyMatch(compactValue::contains);
    }

    private String compact(String value) {
        return value == null
                ? ""
                : value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private String cleanInline(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();

        return cleaned.isBlank() ? null : cleaned;
    }

    private String normalizeBlock(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value
                .replaceAll("[ \\t\\r\\f]+", " ")
                .replaceAll("\\n{3,}", "\\n\\n")
                .trim();

        return cleaned.isBlank() ? null : cleaned;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getClass().getSimpleName() + ": " + current.getMessage();
    }

    private String limitText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }

    private String truncateNullable(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("공식 페이지 SHA-256 해시 생성 실패", exception);
        }
    }

    private record FetchResult(Document document, String method) {
    }

    private record AttachmentDocument(String name, String url) {
    }
}
