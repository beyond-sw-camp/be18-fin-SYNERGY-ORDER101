# # app/service/train_service.py
# from __future__ import annotations

# import subprocess
# import sys
# from pathlib import Path
# from typing import Any, Dict, List

# # train_service.py 위치: app/service/train_service.py
# # BASE = app 디렉토리
# BASE = Path(__file__).resolve().parents[1]
# DATA_PIPELINE = BASE / "data_pipeline"
# MODELING = BASE / "modeling"

# # "없으면 안 되는" 결과 파일들 (07_train_eval.py 가 생성하는 것 포함)
# REQUIRED_FILES = [
#     DATA_PIPELINE / "sample_domain_sales.csv",
#     DATA_PIPELINE / "sample_domain_sales_sku.csv",
#     DATA_PIPELINE / "features_all.csv",
#     MODELING / "lightgbm_model.pkl",
#     # 필요하다면 MODELING / "lightgbm_features.json" 도 추가
# ]

# def _run_script(script: Path, logs: List[str]) -> None:
#     """단일 파이프라인 스크립트 실행 헬퍼 (에러를 RuntimeError로 변환)."""
#     if not script.exists():
#         msg = f"[WARN] script not found, skip: {script}"
#         print(msg)
#         logs.append(msg)
#         return

#     print(f"[PIPELINE] running {script} ...")
#     result = subprocess.run(
#         [sys.executable, str(script)],
#         capture_output=True,
#         text=True,
#     )
#     if result.returncode != 0:
#         err_msg = f"[ERROR] {script.name} failed\n{result.stderr}"
#         print(err_msg)
#         logs.append(err_msg)
#         raise RuntimeError(err_msg)

#     ok_msg = f"[OK] {script.name}"
#     print(ok_msg)
#     logs.append(ok_msg)
#     if result.stdout:
#         logs.append(result.stdout)


# def run_full_pipeline() -> Dict[str, Any]:
#     """
#     02 → 05 → 07 순서대로 전체 파이프라인 실행.
#     - 02_expand_sku_from_category.py
#     - 05_make_features.py
#     - 07_train_eval.py
#     실패해도 예외를 밖으로 던지지 않고 dict 로 상태를 리턴한다.
#     """
#     logs: List[str] = []
#     try:
#         scripts = [
#             DATA_PIPELINE / "02_expand_sku_from_category.py",
#             DATA_PIPELINE / "05_make_features.py",
#             MODELING / "07_train_eval.py",
#         ]

#         for s in scripts:
#             _run_script(s, logs)

#         # 성능 지표를 스크립트에서 파일로 저장했다면 여기서 읽어도 됨.
#         # 지금은 일단 None 으로 두고, 나중에 확장 가능하게 열어둠.
#         return {
#             "status": "success",
#             "logs": logs,
#             "mae": None,
#             "mape": None,
#             "smape": None,
#             "bestIteration": None,
#             "forecastGenerated": False,
#         }
#     except Exception as e:
#         # 에러가 나도 예외 던지지 않고 상태 + 로그만 리턴
#         return {
#             "status": "failed",
#             "error": str(e),
#             "logs": logs,
#             "mae": None,
#             "mape": None,
#             "smape": None,
#             "bestIteration": None,
#             "forecastGenerated": False,
#         }


# def ensure_artifacts_exist() -> Dict[str, Any]:
#     """
#     서버 시작 시 호출.
#     - REQUIRED_FILES 중 하나라도 없으면 run_full_pipeline() 실행
#     - 있으면 아무것도 안 함
#     """
#     missing = [p for p in REQUIRED_FILES if not p.exists()]
#     if not missing:
#         print("[STARTUP] required artifacts already exist. skip training pipeline.")
#         return {"status": "ok", "missing": [], "logs": []}

#     print("[STARTUP] missing artifacts detected:")
#     for m in missing:
#         print(f"  - {m}")

#     print("[STARTUP] running full pipeline to generate artifacts...")
#     result = run_full_pipeline()
#     return result


# def retrain_model() -> Dict[str, Any]:
#     """
#     /internal/ai/model/retrain 에서 호출.
#     그냥 전체 파이프라인을 한 번 더 돌린다.
#     """
#     print("[RETRAIN] starting full pipeline (retrain request)...")
#     result = run_full_pipeline()
#     print("[RETRAIN] pipeline finished with status:", result.get("status"))
#     return result


# app/service/train_service.py
from __future__ import annotations

import subprocess
import sys
from pathlib import Path
from typing import Any, Dict, List

# train_service.py 위치: app/service/train_service.py
# BASE = app 디렉토리
BASE = Path(__file__).resolve().parents[1]
DATA_PIPELINE = BASE / "data_pipeline"
MODELING = BASE / "modeling"

# "없으면 안 되는" 결과 파일들 (07_train_eval.py 가 생성하는 것 포함)
REQUIRED_FILES = [
    DATA_PIPELINE / "sample_domain_sales.csv",
    DATA_PIPELINE / "sample_domain_sales_sku.csv",
    DATA_PIPELINE / "features_all.csv",
    MODELING / "lightgbm_model.pkl",
    # 필요하다면 MODELING / "lightgbm_features.json" 도 추가
]

def _run_script(script: Path, logs: List[str]) -> None:
    """단일 파이프라인 스크립트 실행 헬퍼 (에러를 RuntimeError로 변환)."""
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
    """
    02 → 05 → 07 순서대로 전체 파이프라인 실행.
    - 02_expand_sku_from_category.py
    - 05_make_features.py
    - 07_train_eval.py
    실패해도 예외를 밖으로 던지지 않고 dict 로 상태를 리턴한다.
    """
    logs: List[str] = []
    try:
        scripts = [
            DATA_PIPELINE / "02_expand_sku_from_category.py",
            DATA_PIPELINE / "05_make_features.py",
            MODELING / "07_train_eval.py",
        ]

        for s in scripts:
            _run_script(s, logs)

        # 성능 지표를 스크립트에서 파일로 저장했다면 여기서 읽어도 됨.
        # 지금은 일단 None 으로 두고, 나중에 확장 가능하게 열어둠.
        return {
            "status": "success",
            "logs": logs,
            "mae": None,
            "mape": None,
            "smape": None,
            "bestIteration": None,
            "forecastGenerated": False,
        }
    except Exception as e:
        # 에러가 나도 예외 던지지 않고 상태 + 로그만 리턴
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
    """
    서버 시작 시 호출.
    - REQUIRED_FILES 중 하나라도 없으면 run_full_pipeline() 실행
    - 있으면 아무것도 안 함
    """
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
    """
    /internal/ai/model/retrain 에서 호출.
    그냥 전체 파이프라인을 한 번 더 돌린다.
    """
    print("[RETRAIN] starting full pipeline (retrain request)...")
    result = run_full_pipeline()
    print("[RETRAIN] pipeline finished with status:", result.get("status"))
    return result
