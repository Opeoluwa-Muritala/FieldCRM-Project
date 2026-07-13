"""Transactional email delivery used for staff invitations."""
import html
import json
import logging
from urllib.error import URLError
from urllib.request import Request, urlopen

from app.config import settings

logger = logging.getLogger(__name__)


class EmailService:
    def is_configured(self) -> bool:
        return bool(settings.EMAIL_SERVICE_URL)

    def _deliver(self, *, recipient: str, payload: dict) -> bool:
        if not self.is_configured():
            logger.warning("Email delivery is not configured; message for %s was not sent", recipient)
            return False
        try:
            request = Request(
                settings.EMAIL_SERVICE_URL,
                data=json.dumps(payload).encode("utf-8"),
                headers={"Content-Type": "application/json", "Accept": "application/json"},
                method="POST",
            )
            with urlopen(request, timeout=15) as response:
                if not 200 <= response.status < 300:
                    logger.error("Email service returned status %s for %s", response.status, recipient)
                    return False
            return True
        except (OSError, URLError):
            logger.exception("Unable to send email to %s through the email service", recipient)
            return False

    def send_notification(
        self, *, recipient: str, subject: str, text: str, html_content: str,
        sender_name: str | None = None, reply_email: str | None = None,
    ) -> bool:
        """Send a workflow notification; only conversation emails set reply details."""
        payload = {"to": recipient, "subject": subject, "text": text, "html": html_content}
        if sender_name and reply_email:
            payload.update({"name": sender_name, "reply_email": reply_email})
        return self._deliver(recipient=recipient, payload=payload)

    def send_invitation(self, *, recipient: str, full_name: str, role: str, invitation_url: str) -> bool:
        if not self.is_configured():
            logger.warning("Email delivery is not configured; invitation link for %s: %s", recipient, invitation_url)
            return False
        role_label = role.replace("_", " ").title()
        text = (
            f"Hello {full_name},\n\nYou have been invited to FieldCRM as a {role_label}. "
            "Use this secure link to set your password and activate your account. It expires in 72 hours and can only be used once.\n\n"
            f"{invitation_url}\n\nIf you were not expecting this invitation, you can ignore this email."
        )
        safe_name = html.escape(full_name)
        safe_role = html.escape(role_label)
        safe_url = html.escape(invitation_url, quote=True)
        payload = {
            "to": recipient,
            "subject": "Complete your FieldCRM registration",
            "text": text,
            "html": (
                f"<p>Hello {safe_name},</p><p>You have been invited to FieldCRM as a <strong>{safe_role}</strong>. "
                "Set your password to activate your account. This link expires in 72 hours and can only be used once.</p>"
                f"<p><a href=\"{safe_url}\">Complete registration</a></p>"
                "<p>If you were not expecting this invitation, you can ignore this email.</p>"
            ),
        }
        # Invitations are automated notifications and intentionally do not set
        # a reply contact.
        return self._deliver(recipient=recipient, payload=payload)
