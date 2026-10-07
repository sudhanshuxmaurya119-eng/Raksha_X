import logging
from sqlalchemy import select, func
from database import AsyncSessionLocal
from models.facility import Facility
from models.map_config import MapConfig

logger = logging.getLogger(__name__)

DELHI_NCR_FACILITIES = [
    # ── Police Stations (🚓) ──
    {
        "name": "Connaught Place Police Station",
        "facility_type": "police",
        "address": "B-Block, Connaught Place, New Delhi 110001",
        "phone": "+91 11 2341 2235",
        "latitude": 28.6328,
        "longitude": 77.2197,
        "services": "24/7 Patrol, Women Help Desk, Cyber Crime Desk, Lost Property Desk",
        "emergency_available": True,
        "area": "Connaught Place"
    },
    {
        "name": "Hauz Khas Police Station",
        "facility_type": "police",
        "address": "Aurobindo Marg, Hauz Khas, New Delhi 110016",
        "phone": "+91 11 2686 2110",
        "latitude": 28.5494,
        "longitude": 77.2001,
        "services": "Night Patrol Unit, Women Safety Desk, Emergency PCR Van Dispatch",
        "emergency_available": True,
        "area": "Hauz Khas"
    },
    {
        "name": "Parliament Street Police Station",
        "facility_type": "police",
        "address": "Sansad Marg, Janpath, New Delhi 110001",
        "phone": "+91 11 2336 1100",
        "latitude": 28.6256,
        "longitude": 77.2155,
        "services": "High-Security Zone Command, Emergency Response, Diplomatic Enclave Support",
        "emergency_available": True,
        "area": "Connaught Place"
    },
    {
        "name": "Dwarka Sector 23 Police Station",
        "facility_type": "police",
        "address": "Sector 23, Dwarka, New Delhi 110077",
        "phone": "+91 11 2805 1584",
        "latitude": 28.5714,
        "longitude": 77.0543,
        "services": "Sub-City Rapid Patrol, Anti-Stalking Squad, Highway Surveillance",
        "emergency_available": True,
        "area": "Dwarka"
    },
    {
        "name": "Saket Police Station",
        "facility_type": "police",
        "address": "Press Enclave Marg, Saket, New Delhi 110017",
        "phone": "+91 11 2956 1002",
        "latitude": 28.5244,
        "longitude": 77.2167,
        "services": "Metro Corridor Safety, Commercial Hub Patrol, Women Safety Unit",
        "emergency_available": True,
        "area": "Saket"
    },
    {
        "name": "Noida Sector 20 Police Station",
        "facility_type": "police",
        "address": "Captain Vijyant Thapar Marg, Sector 20, Noida 201301",
        "phone": "+91 120 252 2012",
        "latitude": 28.5823,
        "longitude": 77.3315,
        "services": "Industrial & IT Zone Safety, 112 Dial Response, Women Power Line 1090",
        "emergency_available": True,
        "area": "Noida Sector 18"
    },
    {
        "name": "DLF Cyber City Police Station",
        "facility_type": "police",
        "address": "Phase 2, Cyber Hub, Gurugram, Haryana 122002",
        "phone": "+91 124 238 8802",
        "latitude": 28.4952,
        "longitude": 77.0895,
        "services": "Corporate Night Escort Coordination, Emergency Rapid Response, Cyber Cell",
        "emergency_available": True,
        "area": "Gurugram Cyber City"
    },
    {
        "name": "Rohini South Police Station",
        "facility_type": "police",
        "address": "Sector 3, Rohini, New Delhi 110085",
        "phone": "+91 11 2794 3320",
        "latitude": 28.6990,
        "longitude": 77.1120,
        "services": "Residential Beat Patrol, Senior Citizen & Women Cell, Emergency Dispatch",
        "emergency_available": True,
        "area": "Rohini"
    },

    # ── NGOs (Women Safety, Legal Aid, Shelter, Counselling) (🤝) ──
    {
        "name": "Jagori - Women Safety & Rights Initiative",
        "facility_type": "ngo",
        "address": "B-114, Shivalik, Malviya Nagar, New Delhi 110017",
        "phone": "+91 11 2669 1219",
        "latitude": 28.5367,
        "longitude": 77.2089,
        "services": "Women Safety Audits, Crisis Intervention, Safe City Mapping, Trauma Counselling",
        "emergency_available": True,
        "area": "Hauz Khas"
    },
    {
        "name": "Shakti Vahini Emergency Action Centre",
        "facility_type": "ngo",
        "address": "Basement, 52/1, C.R. Park, New Delhi 110019",
        "phone": "+91 95829 09025",
        "latitude": 28.5385,
        "longitude": 77.2514,
        "services": "Legal Support, Anti-Trafficking Rescue, Emergency Shelter, 24/7 Hotline",
        "emergency_available": True,
        "area": "Saket"
    },
    {
        "name": "Breakthrough India - Youth & Gender Safety Hub",
        "facility_type": "ngo",
        "address": "E-1A, Kailash Colony, Greater Kailash, New Delhi 110048",
        "phone": "+91 11 4166 6101",
        "latitude": 28.5529,
        "longitude": 77.2418,
        "services": "Harassment Bystander Intervention, Community Safety, Legal Consultation",
        "emergency_available": False,
        "area": "Hauz Khas"
    },
    {
        "name": "Saheli Women's Resource & Crisis Centre",
        "facility_type": "ngo",
        "address": "Defenders Building, Nizamuddin West, New Delhi 110013",
        "phone": "+91 11 2431 6837",
        "latitude": 28.5881,
        "longitude": 77.2452,
        "services": "Women Crisis Support, Emergency Safe Stays, Domestic Violence Legal Aid",
        "emergency_available": True,
        "area": "Connaught Place"
    },
    {
        "name": "Sakshi Human Rights & Violence Prevention NGO",
        "facility_type": "ngo",
        "address": "DLF Phase 1, Golf Course Road, Gurugram 122002",
        "phone": "+91 124 405 1888",
        "latitude": 28.4735,
        "longitude": 77.0984,
        "services": "Workplace Harassment Redressal, Legal Assistance, Mental Health First Aid",
        "emergency_available": True,
        "area": "Gurugram Cyber City"
    },
    {
        "name": "Action India Women Shelter & Legal Aid",
        "facility_type": "ngo",
        "address": "Block 5, Subhash Nagar, New Delhi 110027",
        "phone": "+91 11 2513 1475",
        "latitude": 28.6410,
        "longitude": 77.1080,
        "services": "Emergency Short-Stay Shelter, Free Legal Aid, Survivor Rehabilitation",
        "emergency_available": True,
        "area": "Karol Bagh"
    },

    # ── Hospitals & 24/7 Trauma Centers (🏥) ──
    {
        "name": "AIIMS New Delhi - Apex Trauma Centre",
        "facility_type": "hospital",
        "address": "Ansari Nagar East, Ring Road, New Delhi 110029",
        "phone": "+91 11 2658 8500",
        "latitude": 28.5672,
        "longitude": 77.2100,
        "services": "Level 1 Trauma Care, 24/7 Emergency Casualty, Forensic & Medico-Legal Care",
        "emergency_available": True,
        "area": "Hauz Khas"
    },
    {
        "name": "Safdarjung Hospital & Vardhman Mahavir Medical College",
        "facility_type": "hospital",
        "address": "Ring Road, Opp AIIMS, New Delhi 110029",
        "phone": "+91 11 2616 5060",
        "latitude": 28.5702,
        "longitude": 77.2078,
        "services": "24/7 Central Emergency, Specialized Burns Unit, Rape Crisis Medico-Legal Cell",
        "emergency_available": True,
        "area": "Hauz Khas"
    },
    {
        "name": "Max Super Speciality Hospital Saket",
        "facility_type": "hospital",
        "address": "1, 2, Press Enclave Marg, Saket, New Delhi 110017",
        "phone": "+91 11 2651 5050",
        "latitude": 28.5276,
        "longitude": 77.2144,
        "services": "24/7 Advanced Cardiac & Trauma Care, Emergency Ambulance Fleet, ICU Care",
        "emergency_available": True,
        "area": "Saket"
    },
    {
        "name": "Dr. Ram Manohar Lohia (RML) Hospital",
        "facility_type": "hospital",
        "address": "Baba Kharak Singh Marg, Connaught Place, New Delhi 110001",
        "phone": "+91 11 2336 5525",
        "latitude": 28.6247,
        "longitude": 77.2023,
        "services": "24/7 Central Disaster & Trauma Unit, Medico-Legal Emergency, ICU Ambulances",
        "emergency_available": True,
        "area": "Connaught Place"
    },
    {
        "name": "Fortis Memorial Research Institute Gurugram",
        "facility_type": "hospital",
        "address": "Sector 44, Opp HUDA City Centre, Gurugram 122002",
        "phone": "+91 124 496 2200",
        "latitude": 28.4595,
        "longitude": 77.0726,
        "services": "24/7 Advanced Emergency Services, Trauma Surgery, Rapid Ambulance Dispatch",
        "emergency_available": True,
        "area": "Gurugram Cyber City"
    },
    {
        "name": "Jaypee Hospital Noida",
        "facility_type": "hospital",
        "address": "Sector 128, Noida-Greater Noida Expy, Noida 201304",
        "phone": "+91 120 412 2222",
        "latitude": 28.5140,
        "longitude": 77.3690,
        "services": "Expressway Trauma Center, 24/7 Emergency ICU, Stroke & Accident Care",
        "emergency_available": True,
        "area": "Noida Sector 18"
    },

    # ── Fire Stations (🚒) ──
    {
        "name": "Connaught Circus Fire Station",
        "facility_type": "fire_station",
        "address": "Radial Road No 3, Connaught Place, New Delhi 110001",
        "phone": "101",
        "latitude": 28.6341,
        "longitude": 77.2180,
        "services": "24/7 Rapid Fire Rescue, Hazmat Emergency, High-Rise Hydraulic Rescue",
        "emergency_available": True,
        "area": "Connaught Place"
    },
    {
        "name": "Bhikaji Cama Place Fire Station",
        "facility_type": "fire_station",
        "address": "Ring Road, Bhikaji Cama Place, New Delhi 110066",
        "phone": "101",
        "latitude": 28.5668,
        "longitude": 77.1895,
        "services": "South Delhi Emergency Rescue Unit, Highway Accident Rescue",
        "emergency_available": True,
        "area": "Hauz Khas"
    },

    # ── Government Help Centers (🏛️) ──
    {
        "name": "One Stop Centre (Sakhi) for Women - South Delhi",
        "facility_type": "government_center",
        "address": "Near District Court Complex, Saket, New Delhi 110017",
        "phone": "+91 11 2956 5011",
        "latitude": 28.5220,
        "longitude": 77.2175,
        "services": "Integrated Police, Medical, Legal Aid & Temporary Shelter for Women in Distress",
        "emergency_available": True,
        "area": "Saket"
    },
    {
        "name": "Delhi Commission for Women (DCW) Emergency Operations",
        "facility_type": "government_center",
        "address": "C-Block, Vikas Bhawan, I.P. Estate, New Delhi 110002",
        "phone": "181",
        "latitude": 28.6290,
        "longitude": 77.2465,
        "services": "181 Women Helpline Headquarters, Mobile Crisis Intervention Team, Crisis Vans",
        "emergency_available": True,
        "area": "Connaught Place"
    },
    {
        "name": "National Human Rights Commission (NHRC) Help Desk",
        "facility_type": "government_center",
        "address": "Manav Adhikar Bhawan, Block-C, GPO Complex, INA, New Delhi 110023",
        "phone": "+91 11 2465 1330",
        "latitude": 28.5788,
        "longitude": 77.2115,
        "services": "Emergency Rights Violation Reporting, 24/7 Redressal Cell",
        "emergency_available": False,
        "area": "Hauz Khas"
    }
]

# Predefined Geographic Safety Zones across Delhi NCR
PREDEFINED_SAFETY_AREAS = [
    {
        "name": "Connaught Place & Janpath",
        "center_lat": 28.6315,
        "center_lng": 27.2167 if False else 77.2167,
        "radius_meters": 1400.0,
        "default_cases": 4, # LOW
        "default_top_category": "Theft"
    },
    {
        "name": "Hauz Khas & Green Park",
        "center_lat": 28.5494,
        "center_lng": 77.2001,
        "radius_meters": 1600.0,
        "default_cases": 3, # LOW
        "default_top_category": "Harassment"
    },
    {
        "name": "Saket & Press Enclave",
        "center_lat": 28.5255,
        "center_lng": 77.2155,
        "radius_meters": 1500.0,
        "default_cases": 7, # MEDIUM
        "default_top_category": "Harassment"
    },
    {
        "name": "Karol Bagh & Patel Nagar",
        "center_lat": 28.6520,
        "center_lng": 77.1905,
        "radius_meters": 1800.0,
        "default_cases": 18, # HIGH
        "default_top_category": "Theft"
    },
    {
        "name": "Dwarka Sub-City Sector 10-23",
        "center_lat": 28.5740,
        "center_lng": 77.0600,
        "radius_meters": 2200.0,
        "default_cases": 5, # LOW
        "default_top_category": "Suspicious Activity"
    },
    {
        "name": "Rohini Urban Complex",
        "center_lat": 28.7120,
        "center_lng": 77.1180,
        "radius_meters": 2000.0,
        "default_cases": 19, # HIGH
        "default_top_category": "Harassment"
    },
    {
        "name": "Noida Sector 18 & Commercial Hub",
        "center_lat": 28.5700,
        "center_lng": 77.3220,
        "radius_meters": 1700.0,
        "default_cases": 10, # MEDIUM
        "default_top_category": "Harassment"
    },
    {
        "name": "Gurugram Cyber City & DLF",
        "center_lat": 28.4900,
        "center_lng": 77.0850,
        "radius_meters": 1900.0,
        "default_cases": 8, # MEDIUM
        "default_top_category": "Harassment"
    }
]

async def seed_facilities_and_config():
    async with AsyncSessionLocal() as db:
        # 1. Config
        config_res = await db.execute(select(MapConfig).where(MapConfig.key == "default"))
        if not config_res.scalar_one_or_none():
            logger.info("Initializing MapConfig with default risk thresholds (0-5 Low, 6-15 Med, 16+ High)...")
            db.add(MapConfig(
                key="default",
                low_max_cases=5,
                medium_max_cases=15,
                high_min_cases=16
            ))
            await db.commit()

        # 2. Facilities
        fac_count = (await db.execute(select(func.count()).select_from(Facility))).scalar()
        if fac_count == 0:
            logger.info(f"Seeding {len(DELHI_NCR_FACILITIES)} safety facilities across Delhi NCR...")
            for f in DELHI_NCR_FACILITIES:
                db.add(Facility(**f))
            await db.commit()
            logger.info("Safety facilities seeded successfully.")
