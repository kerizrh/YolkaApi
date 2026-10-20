package com.yolka.helpers;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplates {

	public String welcomeTemplate(String userName) {
		return """
				<!DOCTYPE html>
				<html lang="es">
				<head><meta charset="UTF-8"><title>Bienvenido a Yolka</title></head>
				<body style="font-family: Arial, sans-serif; background-color: #F7F9FC; padding: 24px;">
				  <div style="max-width: 520px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; padding: 28px; box-shadow: 0 2px 10px rgba(0,0,0,0.06);">
				    <h2 style="color: #FF7A59; margin-top: 0;">¡Hola, %s! 🐾</h2>
				    <p style="color: #334155; line-height: 1.6;">
				      Tu cuenta en <strong>Yolka</strong> ha sido creada exitosamente. Ahora puedes registrar a tus perros y gatos, llevar su expediente de vacunación y desparasitación, y programar sus rutinas diarias de alimentación y paseo.
				    </p>
				    <p style="color: #64748B; font-size: 13px; margin-top: 24px;">Equipo Yolka — Cuidado y bienestar para tus mascotas</p>
				  </div>
				</body>
				</html>
				""".formatted(userName != null ? userName : "Propietario");
	}

	public String resetPasswordTemplate(String userName, String resetCode) {
		return """
				<!DOCTYPE html>
				<html lang="es">
				<head><meta charset="UTF-8"><title>Recuperación de contraseña - Yolka</title></head>
				<body style="font-family: Arial, sans-serif; background-color: #F7F9FC; padding: 24px;">
				  <div style="max-width: 520px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; padding: 28px; box-shadow: 0 2px 10px rgba(0,0,0,0.06);">
				    <h2 style="color: #FF7A59; margin-top: 0;">Recuperación de contraseña</h2>
				    <p style="color: #334155; line-height: 1.6;">
				      Hola <strong>%s</strong>, recibimos una solicitud para restablecer la contraseña de tu cuenta en Yolka.
				    </p>
				    <div style="background: #FFF3EF; border: 1px dashed #FF7A59; border-radius: 12px; padding: 16px; text-align: center; margin: 20px 0;">
				      <span style="font-size: 24px; font-weight: bold; letter-spacing: 4px; color: #D94E28;">%s</span>
				    </div>
				    <p style="color: #475569; font-size: 14px;">
				      Ingresa este código en la aplicación para definir tu nueva contraseña. El código expira en 15 minutos.
				    </p>
				  </div>
				</body>
				</html>
				""".formatted(userName != null ? userName : "Usuario", resetCode);
	}
}
