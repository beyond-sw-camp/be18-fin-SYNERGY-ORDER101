"""
04_generate_sku_sales.py
- category_weekly_sales.csv → sku-level 주간 판매량 생성
- base_share 를 기본 점유율로 사용
- 시간가변 점유율:
    * base_share
    * 랜덤워크
    * 가격/브랜드 기반 선호도
    * 계절성 (카테고리 수준의 가중치)
    * 프로모션 랜덤 boost
- 출력:
    domain_sales_sku.csv
"""

import numpy as np
import pandas as pd
from pathlib import Path
import re

BASE = Path(__file__).resolve().parent
CAT_SALES = BASE / "category_weekly_sales.csv"
SKU_CAT = BASE / "sku_catalog_ml_with_share.csv"
OUT = BASE / "domain_sales_sku.csv"

GLOBAL_SEED = 42
np.random.seed(GLOBAL_SEED)

# -------------------------------------------------------
# Helper funcs
# -------------------------------------------------------

def safe_float(v):
    try:
        return float(v)
    except:
        return np.nan

def random_walk(prev_logits, base_logits):
    rho = 0.9
    sigma = 0.20
    return (
        rho * prev_logits + 
        (1 - rho) * base_logits +
        np.random.normal(0, sigma, size=len(prev_logits))
    )

def apply_promotion(shares, prob=0.1, boost=0.2):
    mask = np.random.rand(len(shares)) < prob
    shares = shares.copy()
    shares[mask] *= (1 + boost)
    return shares

def lifecycle_multiplier(week_idx):
    ramp = min(1.0, week_idx / 10)
    decay = np.exp(-0.002 * max(0, week_idx - 40))
    return ramp * decay + 0.1

def attribute_preference(row):
    pref = 1.0

    msrp = safe_float(row["msrp_krw"])
    if not np.isnan(msrp):
        if msrp >= 3_000_000:
            pref *= 1.15
        elif msrp >= 1_500_000:
            pref *= 1.05
        else:
            pref *= 0.95

    brand = str(row["brand"]).lower()
    if brand in ["samsung", "lg", "apple"]:
        pref *= 1.20
    elif brand in ["xiaomi"]:
        pref *= 0.95

    return pref

# -------------------------------------------------------
# MAIN
# -------------------------------------------------------

def main():
    df_cat = pd.read_csv(CAT_SALES, parse_dates=["target_date"])
    sku = pd.read_csv(SKU_CAT)

    out_rows = []

    for cat, g in df_cat.groupby("cat_low"):
        sku_sub = sku[sku["cat_low"] == cat]

        if len(sku_sub) == 0:
            continue

        base = sku_sub["base_share"].to_numpy()
        base = base / base.sum()

        base_logits = np.log(base + 1e-9)
        logits = base_logits.copy()

        sku_ids = sku_sub["sku_id"].tolist()

        for idx, row in g.sort_values("target_date").reset_index(drop=True).iterrows():
            logits = random_walk(logits, base_logits)

            if idx == 0:
                pass
            else:
                logits += 0.05 * np.random.randn(len(logits))

            attr_mult = np.array([attribute_preference(r) for _, r in sku_sub.iterrows()])
            logits = logits + np.log(attr_mult + 1e-9)

            shares = np.exp(logits - np.max(logits))
            shares = shares / shares.sum()

            shares = apply_promotion(shares, prob=0.10, boost=0.20)

            week_life = lifecycle_multiplier(idx)
            shares = shares * week_life
            shares = shares / shares.sum()

            qty = (shares * row["actual_order_qty"]).round().astype(int)

            for sid, s, q in zip(sku_ids, shares, qty):
                out_rows.append([
                    1,
                    "본사창고",
                    1,
                    row["cat_low"],
                    sid,
                    row["target_date"].date(),
                    int(row["actual_order_qty"]),
                    s,
                    int(q)
                ])

    out = pd.DataFrame(out_rows, columns=[
        "warehouse_id","region","store_id","cat_low",
        "sku_id","target_date","cat_qty","share_norm","sku_qty"
    ])
    
    out.to_csv(OUT, index=False, encoding="utf-8-sig")
    print(f"Saved → {OUT}")
    print(out.head(20))


if __name__ == "__main__":
    main()
