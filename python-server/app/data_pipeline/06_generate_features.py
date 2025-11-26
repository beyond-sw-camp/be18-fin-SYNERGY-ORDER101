"""
06_generate_features.py
- weekly_sales.csv + weather + sku_catalog_ml_with_share → features_all.csv
- Lag, Moving Average, Time Features 생성
"""

import pandas as pd
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
        df[f"ma_{w}"] = df.groupby("sku_id")["actual_order_qty"].shift(1).rolling(w).mean()
    return df


def main():
    print("Loading weekly sales...")
    sales = pd.read_csv(SALES, parse_dates=["target_date"])

    print("Loading weather...")
    weather = pd.read_csv(WEATHER, parse_dates=["target_date"])

    print("Loading SKU catalog with base share...")
    sku = pd.read_csv(SKU)

    # Merge SKU info
    sales = sales.merge(sku, on="sku_id", how="left")

    # Merge weather
    sales = sales.merge(weather, on="target_date", how="left")

    # Time features
    sales["year"] = sales["target_date"].dt.year
    sales["weekofyear"] = sales["target_date"].dt.isocalendar().week.astype(int)
    sales["month"] = sales["target_date"].dt.month

    # Cyclical encoding
    sales["sin_week"] = np.sin(2 * np.pi * sales["weekofyear"] / 52)
    sales["cos_week"] = np.cos(2 * np.pi * sales["weekofyear"] / 52)

    # Sort for proper lag generation
    sales = sales.sort_values(["sku_id", "target_date"])

    # Add Lags
    sales = add_lags(sales, lags=[1, 2, 4, 8, 12])

    # Add Moving Average
    sales = add_moving_avg(sales, windows=[4, 8, 12])

    # Drop first few NA rows
    sales = sales.dropna().reset_index(drop=True)

    # Reorder
    keep_cols = [
        "target_date", "warehouse_id", "store_id", "sku_id", "actual_order_qty",
        "cat_low", "brand", "minor_option", "msrp_krw", "base_share",
        "avg_temp_c", "cdd", "hdd", "precip_mm", "heat_wave", "cold_wave",
        "year", "weekofyear", "month", "sin_week", "cos_week",
        "lag_1", "lag_2", "lag_4", "lag_8", "lag_12",
        "ma_4", "ma_8", "ma_12"
    ]

    sales = sales[keep_cols]

    # Save
    sales.to_csv(OUT, index=False, encoding="utf-8-sig")

    print(f"Saved features_all.csv → {OUT}")
    print(sales.head(30))


if __name__ == "__main__":
    import numpy as np
    main()
