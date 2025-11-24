from __future__ import annotations

import subprocess
from pathlib import Path
from typing import Dict, Any

import pandas as pd


BASE_DIR = Path(__file__).resolve().parents[1]
DATA_PIPELINE_DIR = BASE_DIR / "data_pipeline"
MODELING_DIR = BASE_DIR / "modeling"

METRICS_CSV = DATA_PIPELINE_DIR / "metrics_eval.csv"
FORECAST_NEXT_CSV = DATA_PIPELINE_DIR / "forecast_next.csv"


def _run_script(script: Path, logs: list[str]) -> None:
    """python 스크립트 하나를 실행하고, 실패 시 RuntimeError 발생"""
    if not script.exists():
        raise RuntimeError(f"script not found: {script}")

    logs.append(f"[TRAIN] === {script.name} 실행 시작 ===")
    res = subprocess.run(
        ["python", str(script)],
        capture_output=True,
        text=True,
    )
    logs.append(f"[TRAIN][{script.name}] STDOUT:\n{res.stdout}")
    if res.stderr:
        logs.append(f"[TRAIN][{script.name}] STDERR:\n{res.stderr}")

    if res.returncode != 0:
        raise RuntimeError(
            f"{script.name} failed with code={res.returncode}. "
            f"stderr={res.stderr.strip()}"
        )

    logs.append(f"[TRAIN] === {script.name} 실행 완료 ===")


def _read_metrics() -> Dict[str, Any]:
    """metrics_eval.csv 에서 mae/mape/smape, best_iteration 읽기"""
    if not METRICS_CSV.exists():
        raise RuntimeError(f"metrics file not found: {METRICS_CSV}")

    df = pd.read_csv(METRICS_CSV)

    def get_metric(name: str):
        row = df.loc[df["metric"] == name, "value"]
        return float(row.iloc[0]) if not row.empty else None

    best_iter = (
        int(df["best_iteration"].max())
        if "best_iteration" in df.columns
        else None
    )

    return {
        "mae": get_metric("mae"),
        "mape": get_metric("mape"),
        "smape": get_metric("smape"),
        "bestIteration": best_iter,
    }


def retrain_model() -> Dict[str, Any]:
    """
    전체 재학습 파이프라인
    1) 02_expand_sku_from_category.py
    2) 05_make_features.py
    3) 07_train_eval.py (LightGBM 학습 + metrics_eval.csv 저장)
    4) 08_predict_future.py (forecast_next.csv 생성)
    """
    logs: list[str] = []
    try:
        # 1) SKU 확장 (필요 없으면 주석 가능)
        script_02 = DATA_PIPELINE_DIR / "02_expand_sku_from_category.py"
        if script_02.exists():
            _run_script(script_02, logs)
        else:
            logs.append(f"[TRAIN] {script_02.name} 없음 → 건너뜀")

        # 2) 피처 생성
        script_05 = DATA_PIPELINE_DIR / "05_make_features.py"
        _run_script(script_05, logs)

        # 3) 모델 학습 & 평가
        script_07 = MODELING_DIR / "07_train_eval.py"
        _run_script(script_07, logs)

        # 4) 향후 HORIZON 주 예측 생성 (선택)
        script_08 = MODELING_DIR / "08_predict_future.py"
        forecast_generated = False
        if script_08.exists():
            try:
                _run_script(script_08, logs)
                forecast_generated = FORECAST_NEXT_CSV.exists()
            except Exception as e:
                logs.append(f"[WARN] {script_08.name} 실행 실패: {e}")
        else:
            logs.append(f"[TRAIN] {script_08.name} 없음 → forecast_next 스킵")

        # 5) metrics 읽기
        metrics = _read_metrics()

        return {
            "status": "success",
            "forecastGenerated": forecast_generated,
            "logs": logs,
            **metrics,
        }

    except Exception as e:
        logs.append(f"[TRAIN][ERROR] retrain_model failed: {e}")
        # 실패 시 status 를 failed 로 넘겨서 main.py 에서 500 으로 올려주기
        return {
            "status": "failed",
            "forecastGenerated": False,
            "logs": logs,
            "error": str(e),
            "mae": None,
            "mape": None,
            "smape": None,
            "bestIteration": None,
        }
