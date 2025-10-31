import os
import base64
from io import BytesIO
from celery import Celery
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas

REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379/0")
celery_app = Celery("report_service", broker=REDIS_URL, backend=REDIS_URL)

@celery_app.task
def generate_pdf_task(title: str, items: list[dict]):
    buffer = BytesIO()
    c = canvas.Canvas(buffer, pagesize=A4)
    width, height = A4

    y = height - 50
    c.setFont("Helvetica-Bold", 16)
    c.drawString(50, y, title)
    y -= 30

    c.setFont("Helvetica", 11)
    for item in items:
        line = f"- {item.get('label', 'Item')}: {item.get('value', '')}"
        c.drawString(50, y, line)
        y -= 18
        if y < 50:
            c.showPage()
            y = height - 50
            c.setFont("Helvetica", 11)

    c.showPage()
    c.save()

    buffer.seek(0)
    pdf_b64 = base64.b64encode(buffer.read()).decode("utf-8")
    return {"pdf_base64": pdf_b64}
