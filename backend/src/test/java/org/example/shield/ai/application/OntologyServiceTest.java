package org.example.shield.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OntologyService 단위 테스트 (Issue #48).
 *
 * <p>Spring 컨텍스트 없이 {@code legal-ontology-slim.json} 을 클래스패스에서
 * 직접 읽어 생성자/@PostConstruct 를 수동 호출한다.</p>
 */
class OntologyServiceTest {

    private OntologyService service;

    @BeforeEach
    void setUp() throws Exception {
        String json;
        try (InputStream in = new ClassPathResource("ontology/legal-ontology-slim.json").getInputStream()) {
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        service = new OntologyService(json, new ObjectMapper());
        service.loadOntology();
    }

    @Test
    @DisplayName("isChildOf — L2 가 L1 의 직계 자식이면 true")
    void isChildOf_directL2_true() {
        assertThat(service.isChildOf("부동산 임대차", "부동산 거래")).isTrue();
    }

    @Test
    @DisplayName("isChildOf — L3 가 L2 의 직계 자식이면 true")
    void isChildOf_directL3_true() {
        assertThat(service.isChildOf("보증금 및 차임", "부동산 임대차")).isTrue();
    }

    @Test
    @DisplayName("isChildOf — 손자 관계(L3↔L1)는 false")
    void isChildOf_grandchild_false() {
        assertThat(service.isChildOf("보증금 및 차임", "부동산 거래")).isFalse();
    }

    @Test
    @DisplayName("isChildOf — 존재하지 않는 name 은 false")
    void isChildOf_unknown_false() {
        assertThat(service.isChildOf("없는노드", "부동산 거래")).isFalse();
        assertThat(service.isChildOf("부동산 임대차", "없는부모")).isFalse();
    }

    @Test
    @DisplayName("isChildOf — null 인자는 false")
    void isChildOf_null_false() {
        assertThat(service.isChildOf(null, "부동산 거래")).isFalse();
        assertThat(service.isChildOf("부동산 임대차", null)).isFalse();
    }

    @Test
    @DisplayName("childrenOf — L1 의 직계 L2 목록을 모두 반환")
    void childrenOf_L1_returnsAllL2() {
        assertThat(service.childrenOf("부동산 거래"))
                .containsExactlyInAnyOrder(
                        "부동산 매매",
                        "부동산 임대차",
                        "부동산 담보",
                        "부동산 권리관계");
    }

    @Test
    @DisplayName("childrenOf — 없는 부모는 빈 리스트")
    void childrenOf_unknown_empty() {
        assertThat(service.childrenOf("없는부모")).isEmpty();
    }
}
