import logging
import json
import os
from typing import Iterable

logger = logging.getLogger(__name__)
_firebase_ready = False


def _ensure_firebase():
    global _firebase_ready
    if _firebase_ready:
        return True
    service_account_json = os.getenv("FIREBASE_SERVICE_ACCOUNT_JSON", "").strip()
    if not service_account_json:
        logger.info("FCM disabled: FIREBASE_SERVICE_ACCOUNT_JSON is not configured")
        return False
    try:
        import firebase_admin
        from firebase_admin import credentials

        if not firebase_admin._apps:
            credential = (
                credentials.Certificate(json.loads(service_account_json))
                if service_account_json.startswith("{")
                else credentials.Certificate(service_account_json)
            )
            firebase_admin.initialize_app(credential)
        _firebase_ready = True
        return True
    except Exception as error:
        logger.warning("FCM initialization failed: %s", error)
        return False


async def send_sos_alert(tokens: Iterable[str], event_id: str, latitude: float | None, longitude: float | None):
    token_list = list(tokens)
    if not token_list or not _ensure_firebase():
        return 0
    try:
        from firebase_admin import messaging

        sent = 0
        for token in token_list:
            message = messaging.Message(
                token=token,
                data={
                    "type": "sos",
                    "event_id": event_id,
                    "latitude": "" if latitude is None else str(latitude),
                    "longitude": "" if longitude is None else str(longitude)
                },
                android=messaging.AndroidConfig(priority="high")
            )
            messaging.send(message)
            sent += 1
        return sent
    except Exception as error:
        logger.warning("FCM SOS delivery failed: %s", error)
        return 0
