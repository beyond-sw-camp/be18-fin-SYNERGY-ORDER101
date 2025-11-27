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


def main():
    print("Loading features...")
    df = pd.read_csv(FEATURES_PATH, parse_dates=["target_date"])

    train = df[df["target_date"] < "2025-01-01"].copy()
    test = df[df["target_date"] >= "2025-01-01"].copy()

    print(f"Train rows: {len(train):,}")
    print(f"Test rows:  {len(test):,}")

    exclude_cols = [
        "actual_order_qty", "target_date", "cat_low",
        "brand", "minor_option"
    ]

    feature_cols = [c for c in df.columns if c not in exclude_cols]

    X_train = train[feature_cols]
    y_train = train["actual_order_qty"]

    X_test = test[feature_cols]
    y_test = test["actual_order_qty"]

    train_ds = lgb.Dataset(X_train, label=y_train)
    valid_ds = lgb.Dataset(X_test, label=y_test)

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

    print("Training model...")

    model = lgb.train(
        params,
        train_ds,
        valid_sets=[valid_ds],
        callbacks=[
            early_stopping(stopping_rounds=200),
            log_evaluation(period=200),
        ],
    )

    joblib.dump(model, MODEL_PATH)
    print(f"Saved model → {MODEL_PATH}")

    # Save feature list
    with open(FEATURES_JSON, "w", encoding="utf-8") as f:
        json.dump(feature_cols, f, ensure_ascii=False, indent=2)

    preds = model.predict(X_test, num_iteration=model.best_iteration)
    mae = np.mean(np.abs(preds - y_test))
    print(f"Test MAE = {mae:.4f}")


if __name__ == "__main__":
    main()
