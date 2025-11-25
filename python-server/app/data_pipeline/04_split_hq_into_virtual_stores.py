"""
04_split_hq_into_virtual_stores.py
- 입력 : hq_weekly.csv (02c_make_hq_weekly.py 출력)
- 처리 : HQ(warehouse_id, sku_id, target_date, actual_order_qty)를
        S개의 가상 가맹점(store_id=1001.., 1001+S-1)으로 안정적으로 분해
        (SKU별 base weight + 주차별 소폭 드리프트)
- 출력 : domain_sales_sku_virtual.csv (기존 domain_sales_sku와 동일 스키마 핵심컬럼)
"""
from pathlib import Path
import numpy as np
import pandas as pd

BASE = Path(__file__).resolve().parent
SRC  = BASE / "hq_weekly.csv"             # (warehouse_id, sku_id, target_date, actual_order_qty)
OUT  = BASE / "domain_sales_sku_virtual.csv"

# 설정
N_STORES = 5                # 가상 가맹점 개수 (원하면 3~10범위로 조정)
STORE_BASE = 1001           # 가상 store_id 시작값
DRIFT_SCALE = 0.05          # 주차별 소폭 드리프트(±5% 수준)
RANDOM_SEED = 42

def dirichlet_like(base, noise):
    """기준 가중치(base)에 소폭 노이즈를 얹어 정규화"""
    w = np.clip(base * (1.0 + noise), 1e-6, None)
    return w / w.sum()

def main():
    if not SRC.exists():
        raise FileNotFoundError(SRC)
    df = pd.read_csv(SRC, parse_dates=["target_date"])
    df["actual_order_qty"] = pd.to_numeric(df["actual_order_qty"], errors="coerce").fillna(0).astype(int)

    # SKU별 base weight (가상점 고정 성향) : Dirichlet로 생성
    rng = np.random.default_rng(RANDOM_SEED)
    sku_list = sorted(df["sku_id"].astype(str).unique().tolist())
    base_w = {}
    for sku in sku_list:
        w = rng.dirichlet(np.ones(N_STORES)).astype(float)
        base_w[sku] = w

    # 분해
    rows = []
    for (wid, sku, dt), g in df.groupby(["warehouse_id","sku_id","target_date"]):
        total = int(g["actual_order_qty"].sum())
        if total <= 0:
            # 그래도 0을 쪼개서 0 유지
            for j in range(N_STORES):
                rows.append({
                    "warehouse_id": wid,
                    "region": "본사창고",
                    "store_id": STORE_BASE + j,
                    "target_date": dt,
                    "cat_low": "",                # 필요시 비워두고 이후 조인으로 보강
                    "sku_id": str(sku),
                    "sku_name_en": "",
                    "sku_name_ko": "",
                    "brand": "",
                    "series": "",
                    "model_code": "",
                    "size_inch": "",
                    "volume_l": "",
                    "capacity_text": "",
                    "energy_grade": "",
                    "price_tier": "",
                    "msrp_krw": np.nan,
                    "launch_date": pd.NaT,
                    "warranty_months": np.nan,
                    "case_pack": np.nan,
                    "min_order_qty": np.nan,
                    "eol_flag": np.nan,
                    "actual_order_qty": 0,
                    "share_norm": np.nan,
                    "sku_qty": 0
                })
            continue

        base = base_w[str(sku)]
        noise = rng.normal(0.0, DRIFT_SCALE, size=N_STORES)
        weights = dirichlet_like(base, noise)

        # 라운딩 합=total 맞추기
        alloc = np.floor(weights * total).astype(int)
        rest = total - alloc.sum()
        if rest > 0:
            # 남은 수량은 가장 큰 잔여분 비중 순서대로 1씩 배분
            frac = (weights * total) - alloc
            order = np.argsort(-frac)
            alloc[order[:rest]] += 1

        for j in range(N_STORES):
            rows.append({
                "warehouse_id": wid,
                "region": "본사창고",
                "store_id": STORE_BASE + j,
                "target_date": dt,
                "cat_low": "",
                "sku_id": str(sku),
                "sku_name_en": "",
                "sku_name_ko": "",
                "brand": "",
                "series": "",
                "model_code": "",
                "size_inch": "",
                "volume_l": "",
                "capacity_text": "",
                "energy_grade": "",
                "price_tier": "",
                "msrp_krw": np.nan,
                "launch_date": pd.NaT,
                "warranty_months": np.nan,
                "case_pack": np.nan,
                "min_order_qty": np.nan,
                "eol_flag": np.nan,
                "actual_order_qty": alloc[j],
                "share_norm": np.nan,
                "sku_qty": alloc[j],
            })

    out = pd.DataFrame(rows).sort_values(["sku_id","target_date","store_id"]).reset_index(drop=True)
    out.to_csv(OUT, index=False)
    print(f"saved: {OUT} ({len(out):,}) with N_STORES={N_STORES}")

if __name__ == "__main__":
    main()
