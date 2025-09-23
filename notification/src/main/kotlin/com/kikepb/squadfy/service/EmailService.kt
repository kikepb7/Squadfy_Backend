package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.type.UserId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.time.Duration

@Service
class EmailService(
    private val javaMailSender: JavaMailSender,
    private val emailTemplateService: EmailTemplateService,
    @param:Value("\${squadfy.email.from}")
    private val emailFrom: String,
    @param:Value("\${squadfy.email.url}")
    private val baseUrl: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun sendHtmlEmail(to: String, subject: String, html: String) {
        val message = javaMailSender.createMimeMessage()
        MimeMessageHelper(message, true, "UTF-8").apply {
            setFrom(emailFrom)
            setTo(to)
            setSubject(subject)
            setText(html, true)
        }

        try {
            javaMailSender.send(message)
        } catch (e: MailException) {
            logger.error("Could not send email", e)
        }
    }

    fun sendVerificationEmail(email: String, username: String, userId: UserId, token: String) {
        logger.info("Sending verification email for user $userId")

        val verificationUrl = UriComponentsBuilder
            .fromUriString("$baseUrl/api/auth/verify")
            .queryParam("token", token)
            .build()
            .toUriString()

        val htmlContent = emailTemplateService.processTemplate(
            templateName = "emails/account-verification",
            variables = mapOf(
                "username" to username,
                "verificationUrl" to verificationUrl
            )
        )

        sendHtmlEmail(
            to = email,
            subject = "Verify your Squadfy account",
            html = htmlContent
        )
    }

    fun sendPasswordResetEmail(email: String, username: String, userId: UserId, token: String, expiresIn: Duration) {
        logger.info("Sending password reset email for user $userId")

        val resetPasswordUrl = UriComponentsBuilder
            .fromUriString("$baseUrl/api/auth/reset-password")
            .queryParam("token", token)
            .build()
            .toUriString()

        val htmlContent = emailTemplateService.processTemplate(
            templateName = "emails/reset-password",
            variables = mapOf(
                "username" to username,
                "verificationUrl" to resetPasswordUrl,
                "expires" to expiresIn.toMinutes()
            )
        )

        sendHtmlEmail(
            to = email,
            subject = "Reset your Squadfy password",
            html = htmlContent
        )
    }
}