package org.example.shield.consultation.domain;

/**
 * 챗봇 시나리오 엔티티 - chatbot_scenarios 테이블 매핑.
 * 법률 분야별로 반드시 수집해야 할 질문 항목을 정의한다.
 *
 * Layer: domain
 * Used by: MessageService
 *
 * TODO: @Entity 구현
 * - id: UUID (PK)
 * - legalField: String (LABOR / CIVIL / CRIMINAL / SCHOOL_VIOLENCE)
 * - questionOrder: int (질문 순서)
 * - question: String ("고용 형태가 정규직인가요?")
 * - fieldName: String ("employment_type" - 수집할 정보의 식별자)
 * - required: boolean (필수 여부)
 * - createdAt: LocalDateTime
 *
 * 예시 데이터:
 * LABOR | 1 | "고용 형태가 정규직인가요, 계약직인가요?" | employment_type  | true
 * LABOR | 2 | "근무 기간이 얼마나 되나요?"              | work_duration    | true
 * LABOR | 3 | "해고 통지를 어떻게 받으셨나요?"           | dismissal_method | true
 * LABOR | 4 | "퇴직금은 수령하셨나요?"                  | severance_pay    | true
 * LABOR | 5 | "관련 증거가 있으신가요?"                  | evidence         | false
 */
public class ChatbotScenario {
}
