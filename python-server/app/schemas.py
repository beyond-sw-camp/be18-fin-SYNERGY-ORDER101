from datetime import date
from pydantic import BaseModel



class ForecastTriggerRequest(BaseModel):
    targetWeek: date


class ForecastTriggerResponse(BaseModel):
    jobType: str
    status: str
    message: str


class RetrainResponse(BaseModel):
    jobType: str = "RETRAIN"
    status: str = "ACCEPTED"
    mae: float | None = None
    mape: float | None = None
    smape: float | None = None
    bestIteration: int | None = None
    forecastGenerated: bool = False
    message: str