package com.example.fileupapi.model;

/**
 * 受付状態を表す列挙型.
 *
 * 現在はRECEIVEDからCOMPLETEDへの遷移を主に使用し、将来拡張用の状態も保持する。
 */
public enum RequestStatus {
    RECEIVED,
    PROCESSING,
    COMPLETED,
    FAILED
}
