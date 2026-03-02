package com.thinkai.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public void sendResetPasswordEmail(String toEmail, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        String subject = "ThinkAI - Đặt lại mật khẩu";
        String htmlContent = """
                <div style="font-family: 'Segoe UI', sans-serif; max-width: 480px; margin: 0 auto; padding: 32px;">
                    <h2 style="color: #1F2937; margin-bottom: 16px;">🎯 ThinkAI</h2>
                    <p style="color: #4B5563; font-size: 15px; line-height: 1.6;">
                        Bạn đã yêu cầu đặt lại mật khẩu. Nhấn nút bên dưới để tạo mật khẩu mới:
                    </p>
                    <div style="text-align: center; margin: 32px 0;">
                        <a href="%s"
                           style="display: inline-block; padding: 14px 32px; background: #C87941;
                                  color: white; text-decoration: none; border-radius: 8px;
                                  font-weight: 600; font-size: 15px;">
                            Đặt lại mật khẩu
                        </a>
                    </div>
                    <p style="color: #9CA3AF; font-size: 13px;">
                        Link này sẽ hết hạn sau <strong>30 phút</strong>.<br>
                        Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
                    </p>
                    <hr style="border: none; border-top: 1px solid #E5E7EB; margin: 24px 0;">
                    <p style="color: #D1D5DB; font-size: 12px;">© 2026 ThinkAI. All rights reserved.</p>
                </div>
                """.formatted(resetLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.", e);
        }
    }
}
