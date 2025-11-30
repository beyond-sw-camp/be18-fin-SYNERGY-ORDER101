import pandas as pd
import random
from pathlib import Path
from datetime import datetime
from app.db import get_connection  
BASE = Path(__file__).resolve().parent
SKU_CSV = BASE / "sku_catalog.csv"
SUPPLIER_OUT = BASE / "supplier_master_load.csv"
PRODUCT_SUPPLIER_OUT = BASE / "product_supplier_load.csv"

SUPPLIER_MAP_FIXED = {
    "APP": 1, "ASU": 2, "CAN": 3, "CUC": 4, "DOL": 5,
    "DYS": 6, "EPS": 7, "LG": 8, "NES": 9, "NET": 10,
    "PHI": 11, "SAM": 12, "TEF": 13, "WIN": 14,
}

def korean_supplier_name(brand_code):
    mapping = {
        "APP": "APPLE 코리아 공급사",
        "ASU": "ASUS 코리아 공급사",
        "CAN": "CANON 코리아 공급사",
        "CUC": "CUCKOO 코리아 공급사",
        "DOL": "DolceGusto 코리아 공급사",
        "DYS": "DYSON 코리아 공급사",
        "EPS": "EPSON 코리아 공급사",
        "LG": "LG전자 공급사",
        "NES": "NESPRESSO 코리아 공급사",
        "NET": "NET 코리아 공급사",
        "PHI": "Philips 코리아 공급사",
        "SAM": "SAMSUNG 코리아 공급사",
        "TEF": "TEFAL 코리아 공급사",
        "WIN": "WINIA 코리아 공급사",
    }
    return mapping.get(brand_code, f"{brand_code} 코리아 공급사")


def random_phone():
    return f"02-{random.randint(100,999)}-{random.randint(1000,9999)}"


def random_address():
    cities = ["서울시 강남구", "서울시 송파구", "서울시 마포구", "경기도 성남시", "경기도 용인시", "인천시 부평구"]
    streets = ["테헤란로", "송파대로", "신촌로", "광화문로", "정자일로", "백현로"]
    return f"{random.choice(cities)} {random.choice(streets)} {random.randint(1,200)}"



def load_product_mapping():
    """
    DB의 product(product_code, product_id)를 불러와 dict로 반환
    ex) {"TV-SAM-55...0001": 60639}
    """
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT product_id, product_code FROM product")
            rows = cur.fetchall()

    # Dict: product_code → product_id
    db_map = {row["product_code"]: row["product_id"] for row in rows}
    print(f"[DB] product 매핑 {len(db_map)}개 로드 완료")
    return db_map


def main():
    df = pd.read_csv(SKU_CSV)
    df["brand_code"] = df["product_code"].apply(lambda x: x.split("-")[1])

    # ----------------------------
    # 1. supplier 생성
    # ----------------------------
    supplier_rows = []
    for brand, sid in SUPPLIER_MAP_FIXED.items():
        supplier_rows.append({
            "supplier_id": sid,
            "supplier_code": f"SUP-{brand}",
            "supplier_name": korean_supplier_name(brand),
            "contact_name": random.choice([
                "조상원","윤석현","박진우","이진구","최유경",
                "노진구","김민주","김민재","이민우","김민석",
                "김동현", "이태연", "조형준"
            ]),
            "contact_number": random_phone(),
            "address": random_address(),
            "created_at": datetime.now().replace(microsecond=0),
            "updated_at": datetime.now().replace(microsecond=0),
        })

    pd.DataFrame(supplier_rows).to_csv(SUPPLIER_OUT, index=False, encoding="utf-8-sig")
    print(f"[OK] supplier CSV 생성 → {SUPPLIER_OUT}")


    ps_rows = []


    db_map = load_product_mapping()

    for _, row in df.iterrows():
        brand = row["brand_code"]
        code = row["product_code"]

        if code not in db_map:
            raise ValueError(f"ERROR: DB에서 해당 product_code 없음 → {code}")

        ps_rows.append({
            "product_supplier_id": 650000 + int(row["product_id"]),
            "product_id": db_map[code],  
            "supplier_id": SUPPLIER_MAP_FIXED[brand],
            "supplier_product_code": f"{SUPPLIER_MAP_FIXED[brand]}-{code}",
            "purchase_price": float(row["price"]) * 0.6,
            "lead_time_days": 2,
            "created_at": datetime.now().replace(microsecond=0),
        })

    pd.DataFrame(ps_rows).to_csv(PRODUCT_SUPPLIER_OUT, index=False, encoding="utf-8-sig")
    print(f"[OK] product_supplier CSV 생성 : {PRODUCT_SUPPLIER_OUT}")


if __name__ == "__main__":
    main()
