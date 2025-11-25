"""
09_overfit_check.py

LightGBM 수요예측 모델이 과적합인지 기본적인 진단을 수행하는 스크립트.

역할:
1) train / test 교집합(중복 row) 존재 여부 확인
2) predictions.csv + features_test.csv 머지해서 실제/예측 값 기준으로
   MAE / MAPE / SMAPE 재계산
3) 단순 baseline (lag_1 그대로 예측) 과 성능 비교
4) 라벨 셔플(label shuffle) 실험으로 leakage(정보유출) 여부 감지

실행:
    poetry run python app/modeling/08_overfit_check.py
"""

from pathlib import Path
import numpy as np
import pandas as pd
from sklearn.metrics import mean_absolute_error

# -------- 경로 설정 --------
BASE = Path(__file__).resolve().parents[1] / "data_pipeline"
TR   = BASE / "features_train.csv"
TE   = BASE / "features_test.csv"
PRED = BASE / "predictions.csv"
METRICS_CSV = BASE / "metrics_eval.csv"

ID_COLS = ["warehouse_id", "store_id", "sku_id", "target_date"]


def smape(y_true, y_pred):
    denom = (np.abs(y_true) + np.abs(y_pred)) / 2.0
    diff = np.abs(y_true - y_pred)
    denom = np.where(denom == 0, 1.0, denom)
    return np.mean(diff / denom) * 100.0


def mape(y_true, y_pred):
    denom = np.where(y_true == 0, 1.0, y_true)
    return np.mean(np.abs((y_true - y_pred) / denom)) * 100.0


def main():
    print("=== [1] 데이터 로드 ===")
    tr = pd.read_csv(TR, parse_dates=["target_date"])
    te = pd.read_csv(TE, parse_dates=["target_date"])
    pred = pd.read_csv(PRED, parse_dates=["target_date"])

    print(f"train rows = {len(tr):,}")
    print(f"test  rows = {len(te):,}")
    print(f"pred  rows = {len(pred):,}")
    print()

    # -------------------------------------------------
    # 1) train / test 교집합 체크
    # -------------------------------------------------
    print("=== [2] Train / Test 교집합(중복 row) 존재 여부 확인 ===")
    id_cols_present = [c for c in ID_COLS if c in tr.columns and c in te.columns]
    if not id_cols_present:
        print("!! 공통 ID 컬럼이 거의 없습니다. 교집합 검사는 건너뜁니다.")
    else:
        key_tr = tr[id_cols_present].drop_duplicates()
        key_te = te[id_cols_present].drop_duplicates()

        merged = key_tr.merge(key_te, on=id_cols_present, how="inner")
        print(f"- train 고유 key 개수: {len(key_tr):,}")
        print(f"- test  고유 key 개수: {len(key_te):,}")
        print(f"- train/test 교집합 key 개수: {len(merged):,}")
        if len(merged) == 0:
            print("  -> OK: train/test key 교집합이 없습니다.")
        else:
            print("  -> ⚠ 경고: train과 test에 동일한 (warehouse, store, sku, date)가 존재합니다.")
    print()

    # -------------------------------------------------
    # 2) predictions.csv + features_test.csv 머지 및 metric 재계산
    # -------------------------------------------------
    print("=== [3] predictions.csv 기준 성능 재계산 ===")
    # merge 기준 ID 컬럼
    id_cols_pred = [c for c in ID_COLS if c in pred.columns and c in te.columns]
    if not id_cols_pred:
        # ID 없이 row order 로만 비교
        print("ID 매칭 컬럼이 없어서 row index 기준으로 정렬 후 비교합니다.")
        te_ = te.reset_index(drop=True)
        pr_ = pred.reset_index(drop=True)
    else:
        print(f"ID 매칭 컬럼: {id_cols_pred}")
        te_ = te.copy()
        pr_ = pred.copy()
        merged = te_.merge(
            pr_[id_cols_pred + ["y_pred"]],
            on=id_cols_pred,
            how="inner",
            suffixes=("", "_predfile")
        )
        if "y_pred" in te_.columns:
            # 혹시 기존 열과 충돌 시
            merged["y_pred_model"] = merged["y_pred_predfile"]
        else:
            merged.rename(columns={"y_pred_predfile": "y_pred_model"}, inplace=True)
        te_ = merged
        pr_ = merged  # 편의상 같은 DF 사용

    if "y" not in te_.columns:
        raise ValueError("features_test.csv 에 'y' 컬럼이 없습니다.")

    if "y_pred_model" in te_.columns:
        y_true = te_["y"].values
        y_pred = te_["y_pred_model"].values
    elif "y_pred" in pr_.columns:
        y_true = te_["y"].values
        y_pred = pr_["y_pred"].values
    else:
        raise ValueError("예측값 y_pred 를 찾을 수 없습니다.")

    mae = mean_absolute_error(y_true, y_pred)
    mp = mape(y_true, y_pred)
    smp = smape(y_true, y_pred)

    print(f"- Recomputed MAE  = {mae:,.4f}")
    print(f"- Recomputed MAPE = {mp:,.2f}%")
    print(f"- Recomputed SMAPE= {smp:,.2f}%")
    print()

    # metrics_eval.csv와 비교
    if METRICS_CSV.exists():
        m = pd.read_csv(METRICS_CSV)
        print("=== [4] metrics_eval.csv 와 비교 ===")
        print(m)
        print()

    # -------------------------------------------------
    # 3) Baseline: lag_1 그대로 쓰는 예측
    # -------------------------------------------------
    print("=== [5] Baseline (lag_1) 성능 계산 ===")
    if "lag_1" not in te.columns:
        print("!! test 데이터에 lag_1 컬럼이 없어 baseline 계산을 건너뜁니다.")
    else:
        mask = ~te["lag_1"].isna()
        y_true_b = te.loc[mask, "y"].values
        y_pred_b = te.loc[mask, "lag_1"].values

        mae_b = mean_absolute_error(y_true_b, y_pred_b)
        mp_b = mape(y_true_b, y_pred_b)
        smp_b = smape(y_true_b, y_pred_b)

        print(f"- Baseline(lag_1) MAE  = {mae_b:,.4f}")
        print(f"- Baseline(lag_1) MAPE = {mp_b:,.2f}%")
        print(f"- Baseline(lag_1) SMAPE= {smp_b:,.2f}%")
        print(f"  (n={len(y_true_b):,} rows)")
    print()

    # -------------------------------------------------
    # 4) Label Shuffle 실험
    # -------------------------------------------------
    print("=== [6] Label Shuffle 실험 (leakage 여부 확인) ===")
    rng = np.random.default_rng(42)
    y_shuffled = y_true.copy()
    rng.shuffle(y_shuffled)

    mae_s = mean_absolute_error(y_shuffled, y_pred)
    mp_s = mape(y_shuffled, y_pred)
    smp_s = smape(y_shuffled, y_pred)

    print(">> y_true 를 랜덤으로 섞은 뒤에도 성능이 비정상적으로 좋다면, 강한 leakage 의심")
    print(f"- Shuffled MAE  = {mae_s:,.4f}")
    print(f"- Shuffled MAPE = {mp_s:,.2f}%")
    print(f"- Shuffled SMAPE= {smp_s:,.2f}%")
    print()

    print("=== [완료] 09_overfit_check.py ===")


if __name__ == "__main__":
    main()
