"""
03_make_hq_weekly.py
- domain_sales_sku.csv (store별 SKU 수요)를 본사(HQ) 기준으로 합산
- 출력: hq_weekly.csv
"""
from pathlib import Path
import pandas as pd

BASE = Path(__file__).resolve().parent
SRC  = BASE / "domain_sales_sku.csv"
OUT  = BASE / "hq_weekly.csv"

def main():
    if not SRC.exists():
        raise FileNotFoundError(f"{SRC} not found")

    df = pd.read_csv(SRC, parse_dates=["target_date"], low_memory=False)
    # HQ 출고량 = 모든 store_id 수요 합산
    hq = (
        df.groupby(["warehouse_id", "sku_id", "target_date"], as_index=False)
          .agg(actual_order_qty=("sku_qty", "sum"))
    )
    hq.to_csv(OUT, index=False)
    print(f"saved: {OUT} ({len(hq):,} rows)")

if __name__ == "__main__":
    main()
