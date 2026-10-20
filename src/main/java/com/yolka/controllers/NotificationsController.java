package com.yolka.controllers;

import com.yolka.dto.UserDtos.CreateNotificationRequest;
import com.yolka.dto.UserDtos.SendPushRequest;
import com.yolka.models.NotificationDocument;
import com.yolka.services.NotificationsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationsController {

	private final NotificationsService notificationsService;

	@GetMapping
	public ResponseEntity<Map<String, Object>> getNotifications(
			@RequestParam(required = false) String userId,
			HttpServletRequest httpRequest
	) {
		String authUserId = (String) httpRequest.getAttribute("authenticatedUserId");
		String targetUserId = (userId != null && !userId.isBlank()) ? userId : authUserId;
		List<NotificationDocument> list = targetUserId != null
				? notificationsService.getUserNotifications(targetUserId)
				: List.of();
		return ResponseEntity.ok(Map.of("success", true, "notifications", list));
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> createNotification(
			@Valid @RequestBody CreateNotificationRequest request,
			HttpServletRequest httpRequest
	) {
		String authUserId = (String) httpRequest.getAttribute("authenticatedUserId");
		CreateNotificationRequest normalized = (request.userId() == null || request.userId().isBlank())
				? new CreateNotificationRequest(
						authUserId,
						request.petId(),
						request.petName(),
						request.category(),
						request.title(),
						request.body(),
						request.scheduledTime(),
						request.timeZone(),
						request.frequencyUnit(),
						request.frequencyInterval(),
						request.daysOfWeek()
				)
				: request;
		NotificationDocument created = notificationsService.createNotification(normalized);
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "notification", created));
	}

	@PostMapping("/send")
	public ResponseEntity<Map<String, Object>> sendPush(@Valid @RequestBody SendPushRequest request) {
		return ResponseEntity.ok(notificationsService.sendManualPush(request));
	}
}
