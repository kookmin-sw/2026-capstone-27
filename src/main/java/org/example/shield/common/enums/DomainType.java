package org.example.shield.common.enums;

/**
 * 법률 분야 ENUM.
 *
 * CIVIL            - 민사 (손해배상, 대여금, 부동산 등)
 * CRIMINAL         - 형사 (사기, 절도, 폭행 등)
 * LABOR            - 노동법 (부당해고, 임금체불 등)
 * SCHOOL_VIOLENCE  - 학교폭력 (학폭 심의, 징계처분 등)
 *
 * null = "잘 모르겠어요"
 */
public enum DomainType {
    CIVIL,
    CRIMINAL,
    LABOR,
    SCHOOL_VIOLENCE
}
