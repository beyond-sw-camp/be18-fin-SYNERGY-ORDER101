import subprocess
from pathlib import Path
from datetime import date

from fastapi import FastAPI, HTTPException, Query
from .schemas import ForecastTriggerResponse, RetrainResponse
from .service.forecast_service import run_forecast_pipeline
from .service.train_service import retrain_model
from app.ops.make_product_master_from_sku import main as build_master
from app.service.product_loader import load_product_master_once



BASE = Path(__file__).resolve().parent

app = FastAPI()

# 서버 시작하면 한번 실행됨
# 필요 파일들 생성
@app.on_event("startup")
def startup_tasks():
    """
    1) sku_catalog.csv -> product_master_load.csv 생성
    2) product_master_load.csv 를 DB product 테이블에 seed
    """
    try:
        print("[STARTUP] build product_master_load from sku_catalog...")
        build_master()
    except FileNotFoundError as e:
        print(f"[WARN] sku_catalog.csv not found. skip build_master(): {e}")
    except Exception as e:
        print(f"[ERROR] build_master() failed: {e}")

    try:
        print("[STARTUP] load product_master_load.csv into DB...")
        load_product_master_once()
    except Exception as e:
        print(f"[ERROR] load_product_master_once() failed: {e}")


# 02, 05, 07 순서대로 실행
@app.get("/api/v1/ai/train")
def run_full_pipeline():
    scripts = [
        BASE / "data_pipeline" / "02_expand_sku_from_category.py",
        BASE / "data_pipeline" / "05_make_features.py",
        BASE / "modeling" / "07_train_eval.py"
    ]
    logs: list[str] = []
    for script in scripts:
        try:
            res = subprocess.run(
                ["python", str(script)],
                capture_output=True, text=True, check=True
            )
            logs.append(f"[OK] {script.name}\n{res.stdout}")
        except subprocess.CalledProcessError as e:
            logs.append(f"[ERROR] {script.name}\n{e.stderr}")
            return {"status": "failed", "log": logs}
    return {"status": "success", "log": logs}





@app.post(
    "/internal/ai/forecasts",
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
    "/internal/ai/model/retrain",
    response_model=RetrainResponse,
    status_code=202,
)
def trigger_retrain():
    """
    Java to Python: 모델 재학습 요청
    """
    try:
        retrain_model()
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Retrain failed: {e}")

    return RetrainResponse(
        jobType="RETRAIN",
        status="ACCEPTED",
        message="Model retrain job completed (or queued).",
    )


@app.get("/health")
def health_check():
    return {"status": "ok"}



if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)