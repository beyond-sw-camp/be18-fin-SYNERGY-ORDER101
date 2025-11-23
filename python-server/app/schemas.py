from datetime import date
from pydantic import BaseModel



class ForecastTriggerRequest(BaseModel):
    targetWeek: date


class ForecastTriggerResponse(BaseModel):
    jobType: str
    status: str
    message: str


class RetrainResponse(BaseModel):
    jobType: str
    status: str
    message: str