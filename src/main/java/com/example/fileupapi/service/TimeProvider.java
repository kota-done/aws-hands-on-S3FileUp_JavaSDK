package com.example.fileupapi.service;

import java.time.Instant;

/**
 * 現在時刻供給部品.
 *
 * 時刻取得を分離し、テスト時に固定時刻へ差し替えやすくする。
 */
public class TimeProvider {
    /**
     * 現在時刻を取得する.
     *
     * @return ISO-8601形式の現在時刻文字列
     */
    public String now() {
        return Instant.now().toString();
    }
}
