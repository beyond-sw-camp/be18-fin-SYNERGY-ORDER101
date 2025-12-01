import pandas as pd
import numpy as np
import joblib
import json
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent / "data_pipeline"

FEA = BASE / "features_all.csv"
MODEL_PATH = BASE / "lightgbm_model.pkl"
FEATURES_JSON = BASE / "lightgbm_features.json"
OUT = BASE / "prediction_2025.csv"


def main():
    print("Loading model...")
    model = joblib.load(MODEL_PATH)

    print("Loading feature list...")
    with open(FEATURES_JSON, "r", encoding="utf-8") as f:
        feature_cols = json.load(f)

    print("Loading features_all.csv...")
    df = pd.read_csv(FEA, parse_dates=["target_date"])


    mask = (df["target_date"] >= "2025-01-06") & \
           (df["target_date"] <= "2025-12-15")
    future = df.loc[mask].copy()


    X = future.reindex(columns=feature_cols, fill_value=0.0)

    print(f"future rows: {len(future):,}")


    y_pred = model.predict(X, num_iteration=getattr(model, "best_iteration", None))
    future["y_pred"] = y_pred.round().astype(int)

    out = future[[
        "warehouse_id",
        "store_id",
        "sku_id",
        "product_code",
        "target_date",
        "y_pred",
    ]].copy()

    out.to_csv(OUT, index=False, encoding="utf-8-sig")
    print(f"[OK] Saved prediction → {OUT} ({len(out):,} rows)")


if __name__ == "__main__":
    main()
