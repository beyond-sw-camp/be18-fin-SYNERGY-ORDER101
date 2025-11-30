import pandas as pd
from pathlib import Path

BASE = Path(__file__).resolve().parent
SRC  = BASE / "sku_catalog.csv"
OUT  = BASE / "product_master_load.csv"

category_map = {
    "TV": 10, "냉장고": 11, "세탁기": 12, "건조기": 13, "청소기": 14,
    "에어컨": 15, "공기청정기": 16, "전자레인지": 17, "오븐": 18,
    "식기세척기": 19, "커피머신": 20, "토스터기": 21, "믹서기": 22,
    "노트북": 23, "데스크탑": 24, "모니터": 25, "스마트폰": 26,
    "스마트워치": 27, "프린터": 28, "라우터": 29
}

def main():
    print("Loading sku_catalog.csv for DB product seed...")
    df = pd.read_csv(SRC)

    if "category_name" not in df.columns:
        raise ValueError("sku_catalog.csv must contain 'category_name'")

    df["product_category_id"] = df["category_name"].map(category_map)

    out = df[[
        "product_code",
        "product_name",
        "price",
        "product_category_id"
    ]].copy()

    out["status"] = 1  # active

    out.to_csv(OUT, index=False, encoding="utf-8-sig")
    print(f"[OK] product_master_load.csv 생성 완료 → {OUT}")
    print(out.head())

if __name__ == "__main__":
    main()
