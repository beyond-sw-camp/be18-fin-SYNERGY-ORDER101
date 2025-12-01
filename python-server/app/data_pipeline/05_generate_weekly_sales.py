"""
05_generate_weekly_sales.py
- domain_sales_sku.csv → weekly_sales.csv
"""

import pandas as pd
from pathlib import Path

BASE = Path(__file__).resolve().parent
SRC = BASE / "domain_sales_sku.csv"
OUT = BASE / "weekly_sales.csv"
EXTRA = BASE / "actual_sales_append.csv"

def main():
    print(f"Loading domain_sales_sku from {SRC} ...")
    df = pd.read_csv(SRC, parse_dates=["target_date"])
    df2 = df.copy()

    if EXTRA.exists():
        print("Loading extra actual sales...")
        extra = pd.read_csv(EXTRA, parse_dates=["target_date"])
        extra_final = extra.rename(columns={
            "product_id": "sku_id",
            "actual_order_qty": "actual_order_qty",
        })[["target_date", "sku_id", "actual_order_qty"]]
        extra_final["warehouse_id"] = 1
        extra_final["store_id"] = 1
        df2 = pd.concat([df2, extra_final], ignore_index=True)

    df2 = df2[[
        "target_date",
        "warehouse_id",
        "store_id",
        "sku_id",
        "sku_qty"
    ]].copy()

    df2 = df2.rename(columns={"sku_qty": "actual_order_qty"})
    df2 = df2.sort_values(["target_date", "sku_id"]).reset_index(drop=True)

    df2.to_csv(OUT, index=False, encoding="utf-8-sig")
    print(f"Saved weekly_sales.csv → {OUT}")
    print(df2.head(20))


if __name__ == "__main__":
    main()
