package br.com.fiap.autorepairshop.workorder.domain;

public enum WorkOrderStatus {
    RECEIVED,
    IN_DIAGNOSIS,
    AWAITING_APPROVAL,
    IN_PROGRESS,
    COMPLETED,
    DELIVERED
}
