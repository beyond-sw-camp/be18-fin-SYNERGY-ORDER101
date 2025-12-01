"""
SKU-level realistic sales generator
- category_weekly_sales.csv → sku-level weekly sales
- 예측 가능하도록 스파이크 감소 + 패턴 유지 + autocorrelation 강화
"""

import numpy as np
import pandas as pd
from pathlib import Path

BASE = Path(__file__).resolve().parent
CAT_SALES = BASE / "category_weekly_sales.csv"
SKU_CAT = BASE / "sku_catalog_ml_with_share.csv"
OUT = BASE / "domain_sales_sku.csv"

np.random.seed(42)

def safe_float(v):
    try:
        return float(v)
    except:
        return np.nan

def attribute_pref(row):
    """brand + price 기반 baseline 판매량 보정"""
    pref = 1.0
    msrp = safe_float(row["msrp_krw"])

    if msrp >= 3_000_000: pref *= 1.15
    elif msrp >= 1_500_000: pref *= 1.05
    else: pref *= 0.90

    brand = str(row["brand"]).lower()
    if brand in ["samsung", "lg", "apple"]: pref *= 1.20
    elif brand in ["xiaomi"]: pref *= 0.90

    return pref

def lifecycle(idx, max_w):
    """초기 증가 → 완만한 유지 → 후반 완만한 감소"""
    ramp = min(1.0, idx / 12)
    decay = 1.0 - max(0, (idx - (max_w*0.7))) / (max_w*0.6)
    decay = max(decay, 0.7)
    return ramp * decay

def main():
    df_cat = pd.read_csv(CAT_SALES, parse_dates=["target_date"])
    sku = pd.read_csv(SKU_CAT)

    out_rows = []
    max_weeks = df_cat["target_date"].nunique()

    for cat, g in df_cat.groupby("cat_low"):
        sku_sub = sku[sku["cat_low"] == cat]
        if len(sku_sub) == 0: continue

        base_share = sku_sub["base_share"].to_numpy()
        base_share = base_share / base_share.sum()

        # baseline_logits = log(share)
        base_logits = np.log(base_share + 1e-9)
        logits = base_logits.copy()

        # SKU 속성 기반 preference
        attr = np.array([attribute_pref(r) for _, r in sku_sub.iterrows()])
        attr = attr / attr.mean()

        sku_ids = sku_sub["sku_id"].tolist()

        for idx, row in g.sort_values("target_date").reset_index(drop=True).iterrows():
            # autocorrelation 강한 랜덤워크 (예측 가능하도록 rho 증가시킴)
            logits = 0.98 * logits + 0.02 * base_logits + np.random.normal(0, 0.015, len(logits))

            # 속성 영향
            logits = logits + np.log(attr + 1e-9)

            # softmax
            shares = np.exp(logits - np.max(logits))
            shares /= shares.sum()

            # lifecycle 적용
            shares *= lifecycle(idx, max_weeks)
            shares /= shares.sum()

            # 판매량
            qty = shares * row["actual_order_qty"]

            # 노이즈 아주 약하게
            qty *= np.random.normal(1.0, 0.05, len(qty))


            qty = shares * row["actual_order_qty"]
            qty = np.maximum(qty, (0.01 * row["actual_order_qty"]))

            qty = np.round(qty).astype(int)
            qty = np.clip(qty, 0, None)

            for sid, s, q in zip(sku_ids, shares, qty):
                out_rows.append([
                    1, "본사창고", 1, row["cat_low"],
                    sid, row["target_date"].date(),
                    int(row["actual_order_qty"]), s, q
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
