import pandas as pd
import numpy as np
import lightgbm as lgb
from lightgbm import early_stopping, log_evaluation
import joblib
from pathlib import Path
import json

# ----------------------------------------
# 경로 설정
# ----------------------------------------
BASE = Path(__file__).resolve().parent.parent / "data_pipeline"
FEATURES_PATH = BASE / "features_all.csv"
MODEL_PATH = BASE / "lightgbm_model.pkl"
FEATURES_JSON = BASE / "lightgbm_features.json"
EVAL_2025_PATH = BASE / "eval_2025_pred_vs_actual.csv"


def main():
    print("Loading features_all.csv ...")
    df = pd.read_csv(FEATURES_PATH, parse_dates=["target_date"])

    # ----------------------------------------
    # Train / Test Split (정확하게 날짜 기반)
    # ----------------------------------------
    train = df[df["target_date"] < "2025-01-01"].copy()     # 21~24년
    test  = df[(df["target_date"] >= "2025-01-01") 
               & (df["target_date"] <= "2025-12-31")].copy()  # 25년

    print(f"Train rows: {len(train):,}")
    print(f"Test rows:  {len(test):,}")
    print(f"Train 기간: {train['target_date'].min()} ~ {train['target_date'].max()}")
    print(f"Test  기간: {test['target_date'].min()} ~ {test['target_date'].max()}")

    # ----------------------------------------
    # Feature 컬럼 선정
    # ----------------------------------------
    exclude_cols = [
        "actual_order_qty",     # 라벨
        "target_date",          # 라벨 X
        "cat_low",
        "brand",
        "minor_option",
        "product_code",
    ]

    # features_all.csv에 존재하는 유효 피처만 자동 선정
    feature_cols = [c for c in df.columns if c not in exclude_cols]

    X_train = train[feature_cols]
    y_train = train["actual_order_qty"]

    X_test = test[feature_cols]
    y_test = test["actual_order_qty"]

    # LightGBM Dataset 구성
    train_ds = lgb.Dataset(X_train, label=y_train)
    valid_ds = lgb.Dataset(X_test, label=y_test)

    # ----------------------------------------
    # 모델 파라미터
    # ----------------------------------------
    params = {
        "objective": "regression",
        "metric": "mae",
        "learning_rate": 0.05,
        "num_leaves": 64,
        "min_data_in_leaf": 50,
        "feature_fraction": 0.9,
        "bagging_fraction": 0.9,
        "bagging_freq": 1,
        "verbose": -1,
    }

    # ----------------------------------------
    # 모델 학습
    # ----------------------------------------
    print("\nTraining LightGBM model...")
    model = lgb.train(
        params,
        train_ds,
        valid_sets=[valid_ds],
        callbacks=[
            early_stopping(stopping_rounds=200),
            log_evaluation(period=200),
        ],
    )

    # 저장
    joblib.dump(model, MODEL_PATH)
    print(f"\n[OK] Saved model → {MODEL_PATH}")

    # Feature List 저장
    with open(FEATURES_JSON, "w", encoding="utf-8") as f:
        json.dump(feature_cols, f, ensure_ascii=False, indent=2)
    print(f"[OK] Saved feature list → {FEATURES_JSON}")

    # ----------------------------------------
    # Test 성능 계산
    # ----------------------------------------
    preds = model.predict(X_test, num_iteration=model.best_iteration)
    mae = np.mean(np.abs(preds - y_test))
    print(f"\n[Test 2025] MAE = {mae:.4f}")

    # ----------------------------------------
    # 25년 실제 vs 예측 저장
    # ----------------------------------------
    eval_df = test[["target_date", "sku_id", "product_code", "actual_order_qty"]].copy()
    eval_df = eval_df.rename(columns={"actual_order_qty": "y"})

    eval_df["y_pred"] = preds

    eval_df.to_csv(EVAL_2025_PATH, index=False)
    print(f"[OK] Saved evaluation file → {EVAL_2025_PATH}")


if __name__ == "__main__":
    main()
