import pandas as pd
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent / "data_pipeline"

PRED = BASE / "predictions.csv"
OUTPUT = BASE / "pred_2025.csv"


def make_pred_2025():
    print("Loading predictions.csv ...")
    pred_df = pd.read_csv(PRED, parse_dates=["target_date"])

    # 2025~ 데이터만 선택
    mask = pred_df["target_date"] >= pd.Timestamp("2025-01-01")

    pred_2025 = pred_df.loc[mask, ["target_date", "product_code", "y_pred"]].copy()

    pred_2025.to_csv(OUTPUT, index=False, encoding="utf-8-sig")
    print(f"[OK] Saved → {OUTPUT}")


if __name__ == "__main__":
    make_pred_2025()
