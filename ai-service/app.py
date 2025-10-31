from fastapi import FastAPI
from pydantic import BaseModel, Field
from typing import List, Optional, Dict

app = FastAPI(title="AI Threat Scoring Service", version="1.0.0")

class ProviderScores(BaseModel):
    virustotal_detected: int = 0
    virustotal_total: int = 0
    shodan_open_ports: int = 0
    shodan_high_risk_ports: int = 0
    hibp_breach_count: int = 0

class Context(BaseModel):
    asset_type: Optional[str] = Field(default=None, description="email|domain|ip|url")

class ScoreRequest(BaseModel):
    provider: ProviderScores
    context: Optional[Context] = None

class Recommendation(BaseModel):
    title: str
    action: str
    priority: str

class ScoreResponse(BaseModel):
    risk_score: int
    risk_level: str
    recommendations: List[Recommendation]

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/score", response_model=ScoreResponse)
def score(req: ScoreRequest):
    vt_ratio = 0.0
    if req.provider.virustotal_total > 0:
        vt_ratio = req.provider.virustotal_detected / max(1, req.provider.virustotal_total)

    shodan_weight = min(1.0, req.provider.shodan_open_ports / 20.0) + (1.0 if req.provider.shodan_high_risk_ports > 0 else 0.0)
    hibp_weight = min(1.0, req.provider.hibp_breach_count / 5.0)

    # Weighted sum -> 0..100
    base = 100.0 * (
        0.5 * vt_ratio +
        0.3 * min(1.0, shodan_weight) +
        0.2 * hibp_weight
    )

    risk_score = int(round(max(0, min(100, base))))
    if risk_score >= 70:
        level = "high"
    elif risk_score >= 40:
        level = "medium"
    else:
        level = "low"

    recs: List[Recommendation] = []
    if req.provider.virustotal_detected > 0:
        recs.append(Recommendation(
            title="Malicious detections on VirusTotal",
            action="Isolated scanning and immediate remediation of flagged artifacts",
            priority="high" if level == "high" else "medium"
        ))
    if req.provider.shodan_open_ports > 0:
        recs.append(Recommendation(
            title="Open services exposed on the Internet",
            action="Close unnecessary ports; restrict exposure; enforce firewall rules",
            priority="high" if req.provider.shodan_high_risk_ports > 0 else "medium"
        ))
    if req.provider.hibp_breach_count > 0:
        recs.append(Recommendation(
            title="Credentials found in public breaches",
            action="Force password reset and enable MFA",
            priority="high"
        ))
    if not recs:
        recs.append(Recommendation(
            title="No pressing findings",
            action="Continue monitoring and apply basic hardening",
            priority="low"
        ))

    return ScoreResponse(risk_score=risk_score, risk_level=level, recommendations=recs)
