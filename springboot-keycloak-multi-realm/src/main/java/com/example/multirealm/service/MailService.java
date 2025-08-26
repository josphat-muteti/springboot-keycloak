package com.example.multirealm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
  private final JavaMailSender mailSender;

  private void send(String to, String subject, String body) {
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setTo(to);
    msg.setSubject(subject);
    msg.setText(body);
    mailSender.send(msg);
  }

  public void sendUserCreated(String to, String username, String tempPassword, String org) {
    send(to, "Your account has been created",
        """
        Hello,

        Your account has been created in organization (realm): %s

        Username: %s
        Temporary password: %s

        Please log in and change your password.

        Regards,
        """.formatted(org, username, tempPassword));
  }

  public void sendUserUpdated(String to, String username, String org) {
    send(to, "Your account was updated",
        """
        Hello %s,

        Your account in organization (realm) "%s" was updated.

        If you didn't request this change, please contact support.

        Regards,
        """.formatted(username, org));
  }

  public void sendUserEnabled(String to, String username, String org) {
    send(to, "Your account has been enabled",
        """
        Hello %s,

        Your account in organization (realm) "%s" has been enabled.

        You can now sign in.

        Regards,
        """.formatted(username, org));
  }

  public void sendUserDisabled(String to, String username, String org) {
    send(to, "Your account has been disabled",
        """
        Hello %s,

        Your account in organization (realm) "%s" has been disabled.

        If this is unexpected, please contact support.

        Regards,
        """.formatted(username, org));
  }

  public void sendUserDeleted(String to, String username, String org) {
    send(to, "Your account has been deleted",
        """
        Hello %s,

        Your account in organization (realm) "%s" has been deleted.

        If this is unexpected, please contact support.

        Regards,
        """.formatted(username, org));
  }
}
