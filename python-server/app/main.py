import subprocess
from pathlib import Path
from datetime import date

from fastapi import FastAPI, HTTPException, Query
from .schemas import ForecastTriggerResponse, RetrainResponse
from .service.forecast_service import run_forecast_pipeline
from .service.train_service import retrain_model
from app.ops.make_product_master_from_sku import main as build_master
from app.service.product_loader import load_product_master_once
from .service.train_service import retrain_model, ensure_artifacts_exist, run_full_pipeline

BASE = Path(__file__).resolve().parent

app = FastAPI()

# 서버 시작하면 한번 실행됨
# 필요 파일들 생성
# @app.on_event("startup")
# def startup_tasks():
#     """
#     1) sku_catalog.csv -> product_master_load.csv 생성
#     2) product_master_load.csv 를 DB product 테이블에 seed
#     """
#     try:
#         print("[STARTUP] build product_master_load from sku_catalog...")
#         build_master()
#     except FileNotFoundError as e:
#         print(f"[WARN] sku_catalog.csv not found. skip build_master(): {e}")
#     except Exception as e:
#         print(f"[ERROR] build_master() failed: {e}")

#     try:
#         print("[STARTUP] load product_master_load.csv into DB...")
#         load_product_master_once()
#     except Exception as e:
#         print(f"[ERROR] load_product_master_once() failed: {e}")
@app.on_event("startup")
def startup_tasks():
    """
    1) sku_catalog.csv -> product_master_load.csv 생성
    2) product_master_load.csv 를 DB product 테이블에 seed
    3) 수요예측에 필요한 CSV/모델 파일 없으면 전체 파이프라인 실행
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

    # 여기 추가
    try:
        print("[STARTUP] ensure AI artifacts exist (features / model etc.)...")
        ensure_artifacts_exist()
    except Exception as e:
        # 어떤 일이 있어도 서버 시작 자체는 막지 않는다.
        print(f"[ERROR] ensure_artifacts_exist() failed: {e}")




# 02, 05, 07 순서대로 실행
# @app.get("/api/v1/ai/train")
# def run_full_pipeline():
#     scripts = [
#         BASE / "data_pipeline" / "02_expand_sku_from_category.py",
#         BASE / "data_pipeline" / "05_make_features.py",
#         BASE / "modeling" / "07_train_eval.py"
#     ]
#     logs: list[str] = []
#     for script in scripts:
#         try:
#             res = subprocess.run(
#                 ["python", str(script)],
#                 capture_output=True, text=True, check=True
#             )
#             logs.append(f"[OK] {script.name}\n{res.stdout}")
#         except subprocess.CalledProcessError as e:
#             logs.append(f"[ERROR] {script.name}\n{e.stderr}")
#             return {"status": "failed", "log": logs}
#     return {"status": "success", "log": logs}
@app.get("/api/v1/ai/train")
def run_full_pipeline_endpoint():
    """
    수동으로 전체 파이프라인을 돌리는 용도 (테스트, 수동 재생성 등).
    내부적으로는 train_service.run_full_pipeline() 을 재사용한다.
    """
    result = run_full_pipeline()
    # 기존 응답 형식(log 키 이름)을 맞추고 싶으면 변환해도 됨.
    return {
        "status": result.get("status"),
        "log": result.get("logs", []),
        "error": result.get("error"),
    }






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



if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)