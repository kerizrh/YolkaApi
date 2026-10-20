package com.yolka.services;

import com.yolka.dto.UserDtos.CreateNotificationRequest;
import com.yolka.dto.UserDtos.SendPushRequest;
import com.yolka.helpers.CustomError;
import com.yolka.helpers.FirebaseAdminHelper;
import com.yolka.models.NotificationDocument;
import com.yolka.models.UserDocument;
import com.yolka.repositories.NotificationRepository;
import com.yolka.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationsService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final FirebaseAdminHelper firebaseAdminHelper;

	public List<NotificationDocument> getUserNotifications(String userId) {
		return notificationRepository.findByUserIdAndDeletedFalseOrderByNextNotificationDateAsc(userId);
	}

	public NotificationDocument createNotification(CreateNotificationRequest request) {
		String tz = (request.timeZone() != null && !request.timeZone().isBlank())
				? request.timeZone()
				: "America/Mexico_City";
		String freqUnit = (request.frequencyUnit() != null && !request.frequencyUnit().isBlank())
				? request.frequencyUnit()
				: "once";
		int freqInterval = (request.frequencyInterval() != null && request.frequencyInterval() > 0)
				? request.frequencyInterval()
				: 1;

		Instant nextDate = calculateNextNotificationDate(request.scheduledTime(), tz, freqUnit, freqInterval);

		NotificationDocument doc = NotificationDocument.builder()
				.userId(request.userId())
				.petId(request.petId())
				.petName(request.petName())
				.category(request.category().toUpperCase())
				.title(request.title())
				.body(request.body() != null ? request.body() : request.title())
				.scheduledTime(request.scheduledTime())
				.nextNotificationDate(nextDate)
				.timeZone(tz)
				.frequencyUnit(freqUnit)
				.frequencyInterval(freqInterval)
				.daysOfWeek(request.daysOfWeek() != null ? request.daysOfWeek() : List.of())
				.active(true)
				.deleted(false)
				.createdAt(Instant.now())
				.build();

		return notificationRepository.save(doc);
	}

	public Map<String, Object> sendManualPush(SendPushRequest request) {
		String targetToken = request.fcmToken();
		if ((targetToken == null || targetToken.isBlank()) && request.userId() != null && !request.userId().isBlank()) {
			UserDocument user = userRepository.findById(request.userId())
					.orElseThrow(() -> CustomError.notFound("Usuario no encontrado", "USER_NOT_FOUND"));
			targetToken = user.getFcmToken();
		}
		if (targetToken == null || targetToken.isBlank()) {
			throw CustomError.badRequest("No se encontró un fcmToken registrado para el usuario", "FCM_TOKEN_MISSING");
		}
		String messageId = firebaseAdminHelper.sendPushNotification(
				targetToken,
				request.title(),
				request.body(),
				request.data()
		);
		return Map.of(
				"success", true,
				"messageId", messageId != null ? messageId : "",
				"message", "Notificación Push enviada correctamente"
		);
	}

	@Scheduled(fixedRate = 60000)
	public void processDueNotifications() {
		Instant now = Instant.now();
		List<NotificationDocument> dueNotifications =
				notificationRepository.findByActiveTrueAndDeletedFalseAndNextNotificationDateLessThanEqual(now);

		for (NotificationDocument notif : dueNotifications) {
			try {
				UserDocument user = notif.getUserId() != null
						? userRepository.findById(notif.getUserId()).orElse(null)
						: null;

				if (user != null && isCategoryEnabledForUser(user, notif.getCategory())) {
					Map<String, String> payload = new HashMap<>();
					payload.put("notificationId", notif.getId());
					payload.put("category", notif.getCategory() != null ? notif.getCategory() : "GENERAL");
					if (notif.getPetId() != null) payload.put("petId", notif.getPetId());
					if (notif.getPetName() != null) payload.put("petName", notif.getPetName());

					if (user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
						firebaseAdminHelper.sendPushNotification(
								user.getFcmToken(),
								notif.getTitle(),
								notif.getBody(),
								payload
						);
					}
				}

				if ("once".equalsIgnoreCase(notif.getFrequencyUnit())) {
					notif.setActive(false);
				} else {
					Instant next = advanceNextDate(
							notif.getNextNotificationDate(),
							notif.getTimeZone(),
							notif.getFrequencyUnit(),
							notif.getFrequencyInterval()
					);
					notif.setNextNotificationDate(next);
				}
				notificationRepository.save(notif);
			} catch (Exception e) {
				log.warn("Error processing scheduled notification {}: {}", notif.getId(), e.getMessage());
			}
		}
	}

	public Instant calculateNextNotificationDate(String scheduledTime, String timeZoneStr, String frequencyUnit, int interval) {
		ZoneId zoneId;
		try {
			zoneId = ZoneId.of(timeZoneStr != null ? timeZoneStr : "America/Mexico_City");
		} catch (Exception e) {
			zoneId = ZoneId.of("America/Mexico_City");
		}

		try {
			if (scheduledTime.matches("^\\d{2}:\\d{2}$")) {
				LocalTime time = LocalTime.parse(scheduledTime, DateTimeFormatter.ofPattern("HH:mm"));
				ZonedDateTime target = ZonedDateTime.now(zoneId).withHour(time.getHour()).withMinute(time.getMinute()).withSecond(0).withNano(0);
				if (target.isBefore(ZonedDateTime.now(zoneId))) {
					target = target.plusDays(1);
				}
				return target.toInstant();
			}
			if (scheduledTime.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
				LocalDate date = LocalDate.parse(scheduledTime);
				return date.atTime(9, 0).atZone(zoneId).toInstant();
			}
			return Instant.parse(scheduledTime);
		} catch (Exception e) {
			try {
				LocalDateTime ldt = LocalDateTime.parse(scheduledTime);
				return ldt.atZone(zoneId).toInstant();
			} catch (Exception ex) {
				return Instant.now().plusSeconds(3600);
			}
		}
	}

	private Instant advanceNextDate(Instant current, String timeZoneStr, String unit, int interval) {
		ZoneId zoneId = ZoneId.of(timeZoneStr != null ? timeZoneStr : "America/Mexico_City");
		ZonedDateTime zdt = ZonedDateTime.ofInstant(current != null ? current : Instant.now(), zoneId);
		int step = Math.max(1, interval);
		ZonedDateTime now = ZonedDateTime.now(zoneId);
		do {
			zdt = switch (unit != null ? unit.toLowerCase() : "days") {
				case "weeks" -> zdt.plusWeeks(step);
				case "months" -> zdt.plusMonths(step);
				case "years" -> zdt.plusYears(step);
				default -> zdt.plusDays(step);
			};
		} while (!zdt.isAfter(now));
		return zdt.toInstant();
	}

	private boolean isCategoryEnabledForUser(UserDocument user, String category) {
		if (user.getPreferences() == null || user.getPreferences().getNotifications() == null || category == null) {
			return true;
		}
		UserDocument.NotificationPreferences prefs = user.getPreferences().getNotifications();
		return switch (category.toUpperCase()) {
			case "VACCINE", "VACCINES" -> prefs.isVaccines();
			case "DEWORMING" -> prefs.isDeworming();
			case "APPOINTMENT", "MEDICAL_APPOINTMENT" -> prefs.isMedicalAppointments();
			case "GROOMING", "GROOMING_BATH", "GROOMING_NAILS" -> prefs.isGrooming();
			case "FEEDING" -> prefs.isFeeding();
			case "WALK", "WALKS" -> prefs.isWalks();
			default -> true;
		};
	}
}
