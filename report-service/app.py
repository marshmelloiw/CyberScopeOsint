import os
from fastapi import FastAPI
from pydantic import BaseModel
from celery.result import AsyncResult
from celery_app import celery_app, generate_pdf_task

app = FastAPI(title="Report Service", version="1.0.0")

class ReportItem(BaseModel):
    label: str
    value: str

class ReportRequest(BaseModel):
    title: str
    items: list[ReportItem]

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/reports")
def create_report(req: ReportRequest):
    task = generate_pdf_task.delay(req.title, [i.model_dump() for i in req.items])
    return {"task_id": task.id}

@app.get("/reports/{task_id}")
def get_report(task_id: str):
    res = AsyncResult(task_id, app=celery_app)
    if res.successful():
        return {"status": "done", **res.result}
    return {"status": res.status}
