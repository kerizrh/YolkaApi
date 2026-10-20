package com.yolka.helpers;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplates {

	public String confirmAccountTemplate(String userName, String confirmationUrl, String token) {
		return """
				<!DOCTYPE html>
				<html lang="es">
				<head><meta charset="UTF-8"><title>Confirma tu cuenta en Yolka</title></head>
				<body style="font-family: Arial, sans-serif; background-color: #F7F9FC; padding: 24px;">
				  <div style="max-width: 520px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; border-top: 12px solid #FF7A59; padding: 28px; box-shadow: 0 2px 10px rgba(0,0,0,0.06);">
				    <h1 style="color: #FF7A59; margin-top: 0; font-size: 28px;">¡Confirma tu cuenta! 🐾</h1>
				    <p style="color: #334155; font-size: 16px; line-height: 1.6;">
				      ¡Hola, <strong>%s</strong>!
				    </p>
				    <p style="color: #334155; line-height: 1.6;">
				      Gracias por registrarte en <strong>Yolka</strong>. Haz clic en el siguiente botón para confirmar tu correo electrónico y activar tu cuenta:
				    </p>
				    <div style="text-align: center; margin: 28px 0;">
				      <a href="%s" target="_blank" style="background-color: #FF7A59; color: #FFFFFF; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-weight: bold; font-size: 16px; display: inline-block;">
				        Confirmar mi cuenta
				      </a>
				    </div>
				    <p style="color: #475569; font-size: 13px; text-align: center;">
				      O utiliza este código/token de confirmación: <strong>%s</strong>
				    </p>
				    <p style="color: #64748B; font-size: 12px; margin-top: 24px; text-align: center;">
				      Este enlace expira en 10 minutos.
				    </p>
				  </div>
				</body>
				</html>
				""".formatted(userName != null ? userName : "Propietario", confirmationUrl, token);
	}

	public String reactivateAccountTemplate(String userName, String confirmationUrl, String token) {
		return """
				<!DOCTYPE html>
				<html lang="es">
				<head><meta charset="UTF-8"><title>Reactivación de tu cuenta en Yolka</title></head>
				<body style="font-family: Arial, sans-serif; background-color: #F7F9FC; padding: 24px;">
				  <div style="max-width: 520px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; border-top: 12px solid #FF7A59; padding: 28px; box-shadow: 0 2px 10px rgba(0,0,0,0.06);">
				    <h1 style="color: #FF7A59; margin-top: 0; font-size: 28px;">¡Bienvenido de vuelta a Yolka! 🐾</h1>
				    <p style="color: #334155; font-size: 16px; line-height: 1.6;">
				      ¡Hola de nuevo, <strong>%s</strong>!
				    </p>
				    <p style="color: #334155; line-height: 1.6;">
				      Tu cuenta ha sido reactivada exitosamente. Haz clic en el siguiente botón para confirmar la reactivación de tu cuenta:
				    </p>
				    <div style="text-align: center; margin: 28px 0;">
				      <a href="%s" target="_blank" style="background-color: #FF7A59; color: #FFFFFF; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-weight: bold; font-size: 16px; display: inline-block;">
				        Confirmar reactivación
				      </a>
				    </div>
				    <p style="color: #475569; font-size: 13px; text-align: center;">
				      O utiliza este código de confirmación: <strong>%s</strong>
				    </p>
				    <p style="color: #64748B; font-size: 12px; margin-top: 24px; text-align: center;">
				      Este enlace expira en 10 minutos.
				    </p>
				  </div>
				</body>
				</html>
				""".formatted(userName != null ? userName : "Propietario", confirmationUrl, token);
	}

	public String confirmedPageHtml(String userName) {
		return """
				<!DOCTYPE html>
				<html lang="es">
				<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Cuenta Confirmada - Yolka</title></head>
				<body style="font-family: Arial, sans-serif; background-color: #F7F9FC; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0;">
				  <div style="max-width: 440px; background: #FFFFFF; border-radius: 16px; border-top: 10px solid #22C55E; padding: 32px; text-align: center; box-shadow: 0 4px 14px rgba(0,0,0,0.08);">
				    <h1 style="color: #16A34A; margin-top: 0;">¡Cuenta confirmada! 🐾</h1>
				    <p style="color: #334155; font-size: 16px; line-height: 1.6;">
				      Hola <strong>%s</strong>, tu correo ha sido verificado exitosamente. Ya puedes volver a la aplicación <strong>Yolka</strong> e iniciar sesión.
				    </p>
				  </div>
				</body>
				</html>
				""".formatted(userName != null ? userName : "Usuario");
	}

	public String resetPasswordTemplate(String userName, String resetCode) {
		return """
				<!DOCTYPE html>
				<html lang="es">
				<head><meta charset="UTF-8"><title>Recuperación de contraseña - Yolka</title></head>
				<body style="font-family: Arial, sans-serif; background-color: #F7F9FC; padding: 24px;">
				  <div style="max-width: 520px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; border-top: 12px solid #FF7A59; padding: 28px; box-shadow: 0 2px 10px rgba(0,0,0,0.06);">
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
