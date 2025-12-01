"""
06_generate_features.py
- weekly_sales.csv + external_weather_weekly.csv + sku_catalog_ml_with_share.csv
  → features_all.csv
"""

import pandas as pd
import numpy as np
from pathlib import Path

BASE = Path(__file__).resolve().parent
SALES = BASE / "weekly_sales.csv"
WEATHER = BASE / "external_weather_weekly.csv"
SKU = BASE / "sku_catalog_ml_with_share.csv"
OUT = BASE / "features_all.csv"


def add_lags(df, lags):
    for l in lags:
        df[f"lag_{l}"] = df.groupby("sku_id")["actual_order_qty"].shift(l)
    return df


def add_moving_avg(df, windows):
    for w in windows:
        df[f"ma_{w}"] = (
            df.groupby("sku_id")["actual_order_qty"]
              .shift(1).rolling(w).mean()
        )
    return df


def main():
    print(f"Loading weekly sales from {SALES} ...")
    sales = pd.read_csv(SALES, parse_dates=["target_date"])

    print(f"Loading weather from {WEATHER} ...")
    weather = pd.read_csv(WEATHER, parse_dates=["target_date"])

    print(f"Loading SKU catalog ML with share from {SKU} ...")
    sku = pd.read_csv(SKU)

    # SKU info merge
    sales = sales.merge(sku, on="sku_id", how="left")

    # weather merge
    sales = sales.merge(weather, on="target_date", how="left")

    # Time features
    sales["year"] = sales["target_date"].dt.year
    sales["weekofyear"] = sales["target_date"].dt.isocalendar().week.astype(int)
    sales["month"] = sales["target_date"].dt.month

    # Cyclical encoding
    sales["sin_week"] = np.sin(2 * np.pi * sales["weekofyear"] / 52)
    sales["cos_week"] = np.cos(2 * np.pi * sales["weekofyear"] / 52)

    # Sort for lags
    sales = sales.sort_values(["sku_id", "target_date"])

    sales = add_lags(sales, lags=[1, 2, 4, 8, 12])
    sales = add_moving_avg(sales, windows=[4, 8, 12])

    sales = sales.dropna().reset_index(drop=True)

    keep_cols = [
        "target_date", "warehouse_id", "store_id", "sku_id", "actual_order_qty",
        "product_code",
        "cat_low", "brand", "minor_option", "msrp_krw", "base_share",
        "avg_temp_c", "cdd", "hdd", "precip_mm", "heat_wave", "cold_wave",
        "year", "weekofyear", "month", "sin_week", "cos_week",
        "lag_1", "lag_2", "lag_4", "lag_8", "lag_12",
        "ma_4", "ma_8", "ma_12"
    ]

    sales = sales[keep_cols]

    sales.to_csv(OUT, index=False, encoding="utf-8-sig")
    print(f"Saved features_all.csv → {OUT}")
    print(sales.head(30))


if __name__ == "__main__":
    main()
