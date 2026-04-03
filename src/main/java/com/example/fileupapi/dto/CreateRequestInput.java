package com.example.fileupapi.dto;

/**
 * 受付作成APIの入力DTO.
 *
 * 受付作成に必要な最小入力項目を定義する。
 */
public record CreateRequestInput(String userId, String fileName) {
}
