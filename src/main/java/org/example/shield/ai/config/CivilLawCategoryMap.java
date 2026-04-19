package org.example.shield.ai.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 민법 카테고리 맵 로더.
 *
 * <p>{@code src/main/resources/seed/civil-law-category-map.yml}을 애플리케이션 시작 시
 * 1회 로드하여, 인제스트 파이프라인(및 장래 retrieval re-ranker)이
 * 조문 번호 → 카테고리 태그 매핑에 사용하도록 한다.</p>
 *
 * <p>YAML은 {@code scripts/generate_civil_law_category_map.py}가 자동 생성하므로
 * 본 클래스는 읽기 전용이다.</p>
 */
@Component
@Getter
@Slf4j
public class CivilLawCategoryMap {

    private static final String RESOURCE_PATH = "seed/civil-law-category-map.yml";

    private Meta meta;
    private List<Range> books = Collections.emptyList();
    private List<ChapterRange> chapters = Collections.emptyList();
    private List<SectionRange> sections = Collections.emptyList();
    private List<MandatoryGroup> mandatoryGroups = Collections.emptyList();

    @PostConstruct
    public void load() {
        ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
        try (InputStream in = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            Root root = yaml.readValue(in, Root.class);
            this.meta = root.meta != null ? root.meta : new Meta();
            this.books = root.books != null ? root.books : Collections.emptyList();
            this.chapters = root.chapters != null ? root.chapters : Collections.emptyList();
            this.sections = root.sections != null ? root.sections : Collections.emptyList();
            this.mandatoryGroups = root.mandatoryGroups != null ? root.mandatoryGroups : Collections.emptyList();
            log.info("민법 카테고리 맵 로드 완료: books={}, chapters={}, sections={}, mandatoryGroups={}",
                    books.size(), chapters.size(), sections.size(), mandatoryGroups.size());
        } catch (Exception e) {
            log.error("민법 카테고리 맵 로드 실패: {}", e.getMessage(), e);
            throw new IllegalStateException("civil-law-category-map.yml 로드 실패", e);
        }
    }

    /**
     * 조문 번호(article_no_int)에 해당하는 카테고리 태그 리스트를 반환.
     *
     * <p>반환 원소:</p>
     * <ul>
     *   <li>{@code book:<편이름>} — 예: {@code book:제1편 총칙}</li>
     *   <li>{@code chapter:<장이름>} — 예: {@code chapter:제1장 통칙}</li>
     *   <li>{@code section:<절이름>} — 해당되는 경우</li>
     *   <li>{@code group:<tag>} — mandatory 그룹 소속 시 (예: {@code group:leasing})</li>
     * </ul>
     */
    public List<String> resolveCategoryIds(int articleNoInt) {
        List<String> result = new ArrayList<>();
        for (Range b : books) {
            if (b.inRange(articleNoInt)) {
                result.add("book:" + b.name);
            }
        }
        for (ChapterRange c : chapters) {
            if (c.inRange(articleNoInt)) {
                result.add("chapter:" + c.name);
            }
        }
        for (SectionRange s : sections) {
            if (s.inRange(articleNoInt)) {
                result.add("section:" + s.name);
            }
        }
        for (MandatoryGroup g : mandatoryGroups) {
            if (g.inRange(articleNoInt)) {
                result.add("group:" + g.tag);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // YAML binding classes
    // ------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Root {
        public Meta meta;
        public List<Range> books;
        public List<ChapterRange> chapters;
        public List<SectionRange> sections;
        @com.fasterxml.jackson.annotation.JsonProperty("mandatory_groups")
        public List<MandatoryGroup> mandatoryGroups;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    public static class Meta {
        public String lawId;
        public String lawName;
        public String sourceLawId;
        public String sourceMst;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    public static class Range {
        public String name;
        public List<Integer> range;

        public boolean inRange(int n) {
            return range != null && range.size() == 2 && n >= range.get(0) && n <= range.get(1);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    public static class ChapterRange extends Range {
        public String book;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    public static class SectionRange extends Range {
        public String book;
        public String chapter;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    public static class MandatoryGroup {
        public String tag;
        public String label;
        public List<Integer> range;
        public String origin;

        public boolean inRange(int n) {
            return range != null && range.size() == 2 && n >= range.get(0) && n <= range.get(1);
        }
    }
}
