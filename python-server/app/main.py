import subprocess
from pathlib import Path
from datetime import date

from fastapi import FastAPI, HTTPException, Query, Body
from .schemas import ForecastTriggerResponse, RetrainResponse
from .service.forecast_service import run_forecast_pipeline
from .service.train_service import retrain_model
from app.ops.make_product_master_from_sku import main as build_master
from app.service.product_loader import load_product_master_once
from .service.train_service import retrain_model, ensure_artifacts_exist, run_full_pipeline
from typing import List

BASE = Path(__file__).resolve().parent

app = FastAPI()

@app.on_event("startup")
def startup_tasks():
    # 1) DB product seed
    try:
        print("[STARTUP] load product_master_load.csv into DB...")
        load_product_master_once()
    except Exception as e:
        print(f"[ERROR] load_product_master_once() failed: {e}")

    # 2) AI artifact 체크
    try:
        print("[STARTUP] ensure AI artifacts exist (features / model etc.)...")
        ensure_artifacts_exist()
    except Exception as e:
        print(f"[ERROR] ensure_artifacts_exist() failed: {e}")



@app.get("/api/v1/ai/train")
def run_full_pipeline_endpoint():
    """
    수동으로 전체 파이프라인을 돌리는 용도 (테스트, 수동 재생성 등).
    내부적으로는 train_service.run_full_pipeline() 을 재사용한다.
    """
    result = run_full_pipeline()

    return {
        "status": result.get("status"),
        "log": result.get("logs", []),
        "error": result.get("error"),
    }


@app.post("/api/v1/internal/ai/add-actual-sales")
def add_actual_sales(data: List[dict] = Body(...)):
    try:
        from app.service.sales_append_service import append_actual_sales
        append_actual_sales(data)
        return {"status": "ok", "rows": len(data)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))



@app.post(
    "/api/v1/internal/ai/forecasts",
    response_model=ForecastTriggerResponse,
    status_code=202,
)
def trigger_forecasts(
    target_week: date | None = Query(None, alias="target_week"),
    targetWeek: date | None = Query(None, alias="targetWeek"),
):
    """
    Java to Python: 수요 예측 실행 요청
    body: { "targetWeek": "YYYY-MM-DD" }
    """
    wk = target_week or targetWeek
    if wk is None:
        raise HTTPException(
            status_code=400,
            detail="query param 'target_week' 또는 'targetWeek' 가 필요합니다.",
        )
    try:
        inserted = run_forecast_pipeline(wk)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Forecast failed: {e}")

    return ForecastTriggerResponse(
        jobType="FORECAST",
        status="ACCEPTED",
        message=f"Inserted {inserted} demand_forecast rows for targetWeek={wk}",
    )



@app.post(
    "/api/v1/internal/ai/model/retrain",
    response_model=RetrainResponse,
    status_code=200,
)
def trigger_retrain():
    result = retrain_model()


    if result.get("status") != "success":
        logs = "\n".join(result.get("logs", []))
        msg = f"Retrain failed: {result.get('error')}\n\nLOGS:\n{logs}"
        raise HTTPException(status_code=500, detail=msg)

    logs = "\n".join(result.get("logs", []))
    msg = "Model retrain completed.\n\n" + logs[:1000]

    return RetrainResponse(
        jobType="RETRAIN",
        status="ACCEPTED",
        mae=result.get("mae"),
        mape=result.get("mape"),
        smape=result.get("smape"),
        bestIteration=result.get("bestIteration"),
        forecastGenerated=result.get("forecastGenerated", False),
        message=msg,
    )


@app.get("/health")
def health_check():
    return {"status": "ok"}

@app.get("/")
def root():
    return {"status": "fastapi running on ec2!!!!!"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)