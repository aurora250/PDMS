package com.pdm.notification.service;

import com.pdm.common.dto.PageResult;
import com.pdm.notification.entity.Alert;

import java.util.List;

public interface NotificationService {

    Alert createAlert(Alert alert);

    Alert markHandled(Long alertId, String handledBy);

    List<Alert> getPendingAlerts();

    PageResult<Alert> search(
            String alertType, String severity, Integer isHandled, int page, int size);

    void scanPermitExpiry();

    void scanVisitOverdue();
}
