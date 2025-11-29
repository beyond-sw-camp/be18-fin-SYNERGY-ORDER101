import pandas as pd
from pathlib import Path
from datetime import datetime

BASE = Path(__file__).resolve().parents[1] / "data_pipeline"
OUT = BASE / "actual_sales_append.csv"

def append_actual_sales(rows: list[dict]):
    df_new = pd.DataFrame(rows)

    df_new = df_new.rename(columns={
        "productId": "product_id",
        "qty": "actual_order_qty",
        "warehouseId": "warehouse_id",
        "userId": "user_id",
        "date": "target_date",
    })

    df_new["target_date"] = pd.to_datetime(df_new["target_date"])

    if OUT.exists():
        df_old = pd.read_csv(OUT, parse_dates=["target_date"])
        df = pd.concat([df_old, df_new], ignore_index=True)
    else:
        df = df_new

    df.to_csv(OUT, index=False, encoding="utf-8-sig")
    print(f"[OK] appended {len(rows)} rows → {OUT}")
