from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.responses import HTMLResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from typing import List
from datetime import datetime
import json
import urllib.parse

from auth_dependencies import get_optional_current_user
from database import get_db
from models.sos_contact import SOSContact
from models.sos_event import SOSEvent
from models.sos_acknowledgement import SOSAcknowledgement
from models.push_token import PushToken
from services.fcm import send_sos_alert
from models.incident import Incident
from models.user import User
from pydantic import BaseModel
from typing import Optional, List

router = APIRouter()

class SOSContactCreate(BaseModel):
    user_id: str
    name: str
    phone: str
    email: Optional[str] = None

class SOSContactResponse(BaseModel):
    id: str
    user_id: str
    name: str
    phone: str
    email: Optional[str] = None
    model_config = {"from_attributes": True}

class SOSTriggerRequest(BaseModel):
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    location_text: Optional[str] = None
    contacts: list = []
    user_id: Optional[str] = None

class SOSAcknowledgeRequest(BaseModel):
    contact_name: str

@router.post("/trigger")
async def trigger_sos(
    request: SOSTriggerRequest,
    db: AsyncSession = Depends(get_db),
    current_user: Optional[User] = Depends(get_optional_current_user),
):
    import logging
    import math
    import os
    logger = logging.getLogger(__name__)
    effective_user_id = current_user.id if current_user else request.user_id

    # 1. Generate WhatsApp Links
    wa_links = []
    contact_count = len(request.contacts) if request.contacts else 0
    message = f"🚨 SOS ALERT 🚨 I need immediate help. My location: {request.location_text} "
    if request.latitude and request.longitude:
        message += f"https://www.google.com/maps?q={request.latitude},{request.longitude}"

    encoded_msg = urllib.parse.quote(message)
    if request.contacts:
        for c in request.contacts:
            # simple cleanup of phone for WA
            phone = ''.join(filter(str.isdigit, c.get("phone", "")))
            if not phone.startswith("91") and len(phone) == 10:
                phone = "91" + phone
            wa_links.append(f"https://wa.me/{phone}?text={encoded_msg}")

    # 2. Log as Incident
    if request.latitude and request.longitude:
        incident = Incident(
            latitude=request.latitude,
            longitude=request.longitude,
            address=request.location_text,
            incident_type="sos_emergency",
            severity=10,
            zone="red",
            status="verified",
            source="sos",
            confidence=1.0,
            ai_summary="SOS Triggered by user",
            description="User pressed SOS button.",
            reported_by=effective_user_id
        )
        db.add(incident)

    # 3. Log SOSEvent
    event = SOSEvent(
        latitude=request.latitude,
        longitude=request.longitude,
        location_text=request.location_text,
        contact_count=contact_count,
        wa_links=json.dumps(wa_links),
        user_id=effective_user_id
    )
    db.add(event)
    await db.commit()
    await db.refresh(event)

    push_sent = 0
    if effective_user_id:
        token_result = await db.execute(
            select(PushToken.token).where(PushToken.user_id == effective_user_id)
        )
        push_sent = await send_sos_alert(
            [token for (token,) in token_result.all()],
            event.id,
            request.latitude,
            request.longitude
        )

    # 4. Integrate Nearby NGOs
    nearby_ngos = []
    # Hackathon Fallback: If browser blocks location, default to Central Delhi
    lat = request.latitude if request.latitude else 28.6139
    lng = request.longitude if request.longitude else 77.2090
    
    try:
        ngo_file = os.path.join(os.path.dirname(__file__), "..", "data", "ngos.json")
        if os.path.exists(ngo_file):
            with open(ngo_file, "r") as f:
                ngos = json.load(f)
            
            def haversine(lat1, lon1, lat2, lon2):
                R = 6371.0 # km
                dlat = math.radians(lat2 - lat1)
                dlon = math.radians(lon2 - lon1)
                a = math.sin(dlat / 2)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon / 2)**2
                c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
                return R * c

            for ngo in ngos:
                dist = haversine(lat, lng, ngo["lat"], ngo["lng"])
                ngo["distance_km"] = round(dist, 1)
                
                # Generate NGO WA Link
                ngo_wa = ngo.get("whatsapp")
                if not ngo_wa and ngo.get("phones"):
                    ngo_wa = ngo["phones"][0]
                
                if ngo_wa:
                    ngo_phone = ''.join(filter(str.isdigit, ngo_wa))
                    if not ngo_phone.startswith("91") and len(ngo_phone) == 10:
                        ngo_phone = "91" + ngo_phone
                    
                    ngo_msg = f"🚨 NGO ALERT from AuroraSafe 🚨\nEmergency: SOS Activated\nLocation: https://www.google.com/maps?q={lat},{lng}\nPlease assist immediately."
                    ngo["wa_link"] = f"https://wa.me/{ngo_phone}?text={urllib.parse.quote(ngo_msg)}"
                else:
                    ngo["wa_link"] = None
            
            ngos.sort(key=lambda x: x["distance_km"])
            nearby_ngos = ngos[:3]
    except Exception as e:
        logger.error(f"Failed to process NGOs: {e}")

    logger.warning(f"SOS TRIGGERED at {request.latitude}, {request.longitude} | {request.location_text} | contacts: {contact_count}")

    return {
        "status": "success",
        "message": "SOS alert triggered. Contacts and NGOs have been identified.",
        "timestamp": datetime.utcnow().isoformat(),
        "wa_links": wa_links,
        "location": {"lat": request.latitude, "lng": request.longitude},
        "event_id": event.id,
        "push_sent": push_sent,
        "nearby_ngos": nearby_ngos
    }

@router.post("/{event_id}/acknowledge")
async def acknowledge_sos(
    event_id: str,
    request: SOSAcknowledgeRequest,
    db: AsyncSession = Depends(get_db),
    _: Optional[User] = Depends(get_optional_current_user),
):
    event = await db.get(SOSEvent, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="SOS event not found")

    acknowledgement = SOSAcknowledgement(
        event_id=event_id,
        contact_name=request.contact_name.strip() or "Trusted contact"
    )
    db.add(acknowledgement)
    await db.commit()
    await db.refresh(acknowledgement)
    return {
        "status": "acknowledged",
        "event_id": event_id,
        "contact_name": acknowledgement.contact_name,
        "acknowledged_at": acknowledgement.created_at.isoformat()
    }

@router.get("/{event_id}/acknowledge", response_class=HTMLResponse)
@router.get("/ack/{event_id}", response_class=HTMLResponse)
async def acknowledge_sos_page(
    event_id: str,
    contact_name: Optional[str] = Query("Trusted Contact", alias="contact_name"),
    contact: Optional[str] = Query(None, alias="contact"),
    db: AsyncSession = Depends(get_db),
):
    effective_name = (contact or contact_name or "Trusted Contact").strip()
    event = await db.get(SOSEvent, event_id)
    
    # Record acknowledgement if event exists
    if event:
        acknowledgement = SOSAcknowledgement(
            event_id=event_id,
            contact_name=effective_name
        )
        db.add(acknowledgement)
        await db.commit()
    
    loc_display = event.location_text if (event and event.location_text) else "Location tracking active"
    maps_btn = ""
    if event and event.latitude and event.longitude:
        maps_url = f"https://www.google.com/maps?q={event.latitude},{event.longitude}"
        maps_btn = f'''
        <a href="{maps_url}" target="_blank" style="display:inline-block; margin-top:16px; padding:12px 24px; background:#ef4444; color:#fff; text-decoration:none; border-radius:10px; font-weight:700; font-size:15px; box-shadow:0 4px 14px rgba(239,68,68,0.4);">
            📍 Open Victim Location in Google Maps
        </a>
        '''

    html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>RakshaX SOS Alert Confirmed</title>
    <style>
        body {{
            margin: 0;
            padding: 20px;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background: #0a0f1d;
            color: #f8fafc;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 90vh;
        }}
        .card {{
            background: #151d30;
            border: 1px solid #10b981;
            border-radius: 20px;
            padding: 32px 24px;
            max-width: 480px;
            width: 100%;
            text-align: center;
            box-shadow: 0 10px 30px rgba(16, 185, 129, 0.15);
        }}
        .badge {{
            width: 64px;
            height: 64px;
            background: rgba(16, 185, 129, 0.2);
            color: #10b981;
            border: 2px solid #10b981;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 32px;
            margin: 0 auto 20px auto;
        }}
        h1 {{
            font-size: 22px;
            font-weight: 800;
            color: #10b981;
            margin: 0 0 10px 0;
            letter-spacing: 0.5px;
        }}
        p {{
            color: #94a3b8;
            font-size: 14px;
            line-height: 1.6;
            margin: 8px 0;
        }}
        .contact-box {{
            background: #1e293b;
            border-radius: 12px;
            padding: 14px;
            margin: 20px 0;
            border: 1px solid #334155;
            text-align: left;
        }}
        .contact-box b {{
            color: #f1f5f9;
        }}
        .note {{
            font-size: 12px;
            color: #64748b;
            margin-top: 24px;
        }}
    </style>
</head>
<body>
    <div class="card">
        <div class="badge">✓</div>
        <h1>SOS ALERT CONFIRMED</h1>
        <p>Thank you, <b>{effective_name}</b>! Your confirmation has been received.</p>
        <p>The victim's phone and trusted network have been notified that you are actively responding to this emergency.</p>
        
        <div class="contact-box">
            <p><b>Event ID:</b> {event_id}</p>
            <p><b>Last Reported Location:</b> {loc_display}</p>
            <p><b>Status:</b> <span style="color:#10b981; font-weight:700;">RESPONDING / ACKNOWLEDGED</span></p>
        </div>

        {maps_btn}

        <p class="note">RakshaX Emergency Lifeline • In extreme life-threatening danger, dial national emergency <b>112</b> immediately.</p>
    </div>
</body>
</html>"""
    return HTMLResponse(content=html_content)

@router.get("/{event_id}/status")
async def get_sos_status(
    event_id: str,
    db: AsyncSession = Depends(get_db),
    _: Optional[User] = Depends(get_optional_current_user),
):
    event = await db.get(SOSEvent, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="SOS event not found")

    result = await db.execute(
        select(SOSAcknowledgement)
        .where(SOSAcknowledgement.event_id == event_id)
        .order_by(SOSAcknowledgement.created_at.asc())
    )
    acknowledgements = result.scalars().all()
    return {
        "event_id": event_id,
        "status": "acknowledged" if acknowledgements else "pending",
        "acknowledgements": [
            {
                "contact_name": item.contact_name,
                "acknowledged_at": item.created_at.isoformat()
            }
            for item in acknowledgements
        ]
    }

@router.get("/history")
async def get_sos_history(user_id: str = None, db: AsyncSession = Depends(get_db)):
    query = select(SOSEvent).order_by(SOSEvent.created_at.desc())
    if user_id:
        query = query.where(SOSEvent.user_id == user_id)
    query = query.limit(10)
    result = await db.execute(query)
    events = result.scalars().all()
    out = []
    for e in events:
        try:
            links = json.loads(e.wa_links)
        except:
            links = []
        out.append({
            "id": e.id,
            "latitude": e.latitude,
            "longitude": e.longitude,
            "location_text": e.location_text,
            "contact_count": e.contact_count,
            "wa_links": links,
            "created_at": e.created_at.isoformat()
        })
    return out

@router.get("/contacts/{user_id}", response_model=List[SOSContactResponse])
async def get_contacts(user_id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(SOSContact).where(SOSContact.user_id == user_id))
    return result.scalars().all()

@router.post("/contacts", response_model=SOSContactResponse)
async def add_contact(contact: SOSContactCreate, db: AsyncSession = Depends(get_db)):
    db_contact = SOSContact(**contact.model_dump())
    db.add(db_contact)
    await db.commit()
    await db.refresh(db_contact)
    return db_contact

@router.delete("/contacts/{contact_id}")
async def delete_contact(contact_id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(SOSContact).where(SOSContact.id == contact_id))
    contact = result.scalars().first()
    if not contact:
        raise HTTPException(status_code=404, detail="Contact not found")
    await db.delete(contact)
    await db.commit()
    return {"message": "Contact deleted"}
