import pandas as pd
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent / "data_pipeline"

FEATURES = BASE / "features_all.csv"
OUTPUT = BASE / "actual_2025.csv"


def make_actual_2025():
    print("Loading features_all.csv ...")
    df = pd.read_csv(FEATURES, parse_dates=["target_date"])

    # 2025년 기간 필터 — and 가 아니라 & !!
    mask = (df["target_date"] >= pd.Timestamp("2025-01-01")) & \
           (df["target_date"] <= pd.Timestamp("2025-12-15"))

    actual_df = df.loc[mask, ["target_date", "product_code", "actual_order_qty"]].copy()

    # NaN 처리
    actual_df["actual_order_qty"] = actual_df["actual_order_qty"].fillna(0)

    actual_df.to_csv(OUTPUT, index=False, encoding="utf-8-sig")
    print(f"[OK] Saved → {OUTPUT}")


if __name__ == "__main__":
    make_actual_2025()
