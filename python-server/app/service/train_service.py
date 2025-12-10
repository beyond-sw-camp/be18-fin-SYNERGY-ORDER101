from __future__ import annotations

import subprocess
import sys
from pathlib import Path
from typing import Any, Dict, List

BASE = Path(__file__).resolve().parents[1]
DATA_PIPELINE = BASE / "data_pipeline"
MODELING = BASE / "modeling"

REQUIRED_FILES = [
    DATA_PIPELINE / "features_all.csv",
    DATA_PIPELINE / "lightgbm_model.pkl",
    DATA_PIPELINE / "catboost_model.pkl",
    DATA_PIPELINE / "predictions.csv",
]


def _run_script(script: Path, logs: List[str]) -> None:
    if not script.exists():
        msg = f"[WARN] script not found, skip: {script}"
        print(msg)
        logs.append(msg)
        return

    print(f"[PIPELINE] running {script} ...")
    result = subprocess.run(
        [sys.executable, str(script)],
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        err_msg = f"[ERROR] {script.name} failed\n{result.stderr}"
        print(err_msg)
        logs.append(err_msg)
        raise RuntimeError(err_msg)

    ok_msg = f"[OK] {script.name}"
    print(ok_msg)
    logs.append(ok_msg)
    if result.stdout:
        logs.append(result.stdout)


def run_full_pipeline() -> Dict[str, Any]:
    logs: List[str] = []
    try:
        scripts = [
            # DATA_PIPELINE / "00_generate_sku_catalog.py", 
            DATA_PIPELINE / "01_generate_base_share.py",
            DATA_PIPELINE / "02_generate_synthetic_weather.py",
            # DATA_PIPELINE / "03_generate_category_sales.py",  # category_weekly_sales.csv 수동 관리
            DATA_PIPELINE / "04_generate_sku_sales.py",
            DATA_PIPELINE / "05_generate_weekly_sales.py",
            DATA_PIPELINE / "06_generate_features.py",
            MODELING / "07_train_model.py",
            MODELING / "08_predict_future.py",
        ]

        for s in scripts:
            _run_script(s, logs)

        return {
            "status": "success",
            "logs": logs,
            "mae": None,
            "mape": None,
            "smape": None,
            "bestIteration": None,
            "forecastGenerated": True,
        }
    except Exception as e:
        return {
            "status": "failed",
            "error": str(e),
            "logs": logs,
            "mae": None,
            "mape": None,
            "smape": None,
            "bestIteration": None,
            "forecastGenerated": False,
        }


def ensure_artifacts_exist() -> Dict[str, Any]:
    missing = [p for p in REQUIRED_FILES if not p.exists()]
    if not missing:
        print("[STARTUP] required artifacts already exist. skip training pipeline.")
        return {"status": "ok", "missing": [], "logs": []}

    print("[STARTUP] missing artifacts detected:")
    for m in missing:
        print(f"  - {m}")

    print("[STARTUP] running full pipeline to generate artifacts...")
    result = run_full_pipeline()
    return result


def retrain_model() -> Dict[str, Any]:
    print("[RETRAIN] starting full pipeline (retrain request)...")
    result = run_full_pipeline()
    print("[RETRAIN] pipeline finished with status:", result.get("status"))
    return result
