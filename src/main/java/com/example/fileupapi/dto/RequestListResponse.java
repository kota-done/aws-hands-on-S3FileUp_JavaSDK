package com.example.fileupapi.dto;

import java.util.List;

/**
 * 受付一覧取得APIの出力DTO.
 *
 * 複数受付の状態確認を一度に返却するためのコンテナとして使用する。
 */
public record RequestListResponse(List<RequestDetailResponse> items) {
}
