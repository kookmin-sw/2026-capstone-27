package org.example.shield.common.response;

/**
 * 공통 API 응답 래퍼.
 * 모든 API가 { result: true/false, message: "...", data: {...} } 형식으로 응답.
 *
 * TODO:
 * - result: boolean
 * - message: String
 * - data: T (제네릭)
 * - static ok(data): 성공 응답
 * - static ok(data, message): 성공 응답 + 메시지
 * - static error(message): 에러 응답
 */
public class ApiResponse {
}
