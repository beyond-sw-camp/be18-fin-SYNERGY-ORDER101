import pandas as pd
import numpy as np
import lightgbm as lgb
from lightgbm import early_stopping, log_evaluation
import joblib
from pathlib import Path
import json

BASE = Path(__file__).resolve().parent.parent / "data_pipeline"
FEATURES_PATH = BASE / "features_all.csv"
MODEL_PATH = BASE / "lightgbm_model.pkl"
FEATURES_JSON = BASE / "lightgbm_features.json"
EVAL_2025_PATH = BASE / "eval_2025_pred_vs_actual.csv"


def main():
    print("Loading features_all.csv ...")
    df = pd.read_csv(FEATURES_PATH, parse_dates=["target_date"])

    train = df[df["target_date"] < "2025-01-01"].copy()
    test  = df[(df["target_date"] >= "2025-01-01") &
               (df["target_date"] <= "2025-12-31")].copy()

    print(f"Train rows: {len(train):,}")
    print(f"Test rows:  {len(test):,}")

    # feature에서 제거
    REMOVE_COLS = [
        "actual_order_qty", "target_date",
        "cat_low", "brand", "minor_option", "product_code",
        "sku_id",     
        "warehouse_id", "store_id"  
    ]

    # 중요도 기반 Top5 피처만 사용
    TOP5_FEATURES = [
        "lag_1", "lag_2", "ma_12", "base_share", "ma_8"
    ]

    # df에 존재하는 컬럼 중 Top5만 선택
    feature_cols = [c for c in TOP5_FEATURES if c in df.columns]

    print("\n사용 Feature 컬럼:")
    print(feature_cols)

    X_train = train[feature_cols]
    y_train = train["actual_order_qty"]
    X_test = test[feature_cols]
    y_test = test["actual_order_qty"]

    params = {
        "objective": "regression",
        "metric": "l2",
        "learning_rate": 0.05,
        "num_leaves": 48,
        "min_data_in_leaf": 40,
        "feature_fraction": 1.0,
        "bagging_fraction": 0.9,
        "bagging_freq": 1,
        "verbose": -1,
    }

    print("\nTraining LightGBM model...")
    train_ds = lgb.Dataset(X_train, label=y_train)

    model = lgb.train(
        params,
        train_ds,
        valid_sets=[lgb.Dataset(X_test, label=y_test)],
        callbacks=[
            early_stopping(stopping_rounds=200),
            log_evaluation(period=200),
        ],
    )

    joblib.dump(model, MODEL_PATH)
    print(f"[OK] Saved model → {MODEL_PATH}")

    with open(FEATURES_JSON, "w", encoding="utf-8") as f:
        json.dump(feature_cols, f, ensure_ascii=False, indent=2)
    print(f"[OK] Saved feature list → {FEATURES_JSON}")

    preds = model.predict(X_test, num_iteration=model.best_iteration)
    mae = np.mean(np.abs(preds - y_test))
    print(f"\n[Test 2025] MAE = {mae:.4f}")

    eval_df = test[["target_date", "sku_id", "actual_order_qty"]].copy()
    eval_df = eval_df.rename(columns={"actual_order_qty": "y"})
    eval_df["y_pred"] = preds
    eval_df.to_csv(EVAL_2025_PATH, index=False)
    print(f"[OK] Saved evaluation file → {EVAL_2025_PATH}")


if __name__ == "__main__":
    main()
