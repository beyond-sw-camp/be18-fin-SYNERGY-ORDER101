import pandas as pd
import random
from pathlib import Path
from datetime import datetime

BASE = Path(__file__).resolve().parent
SKU_CSV = BASE / "sku_catalog.csv"
SUPPLIER_OUT = BASE / "supplier_master_load.csv"
PRODUCT_SUPPLIER_OUT = BASE / "product_supplier_load.csv"


KOREAN_NAMES = [
    "조상원","윤석현","박진우","이진구","최유경","한지민","장유진","서동현","윤아름","고재현",
    "강수진","신민호","문지현","임도현","권예린","차정우","정유나","황민재","송다영","백서준"
]


def korean_supplier_name(brand):
    simple = brand.upper()

    # 특별한 예외 처리
    mapping = {
        "SAMSUNG": "삼성전자 공급사",
        "LG": "LG전자 공급사",
        "APPLE": "애플코리아 공급사",
        "SONY": "소니코리아 공급사",
        "PANASONIC": "파나소닉코리아 공급사",
        "BOSCH": "보쉬코리아 공급사",
        "ELECTROLUX": "일렉트로룩스코리아 공급사",
        "XIAOMI": "샤오미코리아 공급사",
        "TCL": "TCL 코리아 공급사",
        "WINIA": "위니아 공급사",
        "DELONGHI": "드롱기코리아 공급사",
        "BREVILLE": "브레빌코리아 공급사",
        "PHILIPS": "필립스코리아 공급사",
        "BLAUPUNKT": "블라우풍트코리아 공급사",
        "CUCKOO": "쿠쿠전자 공급사",
        "CUCHEN": "쿠첸 공급사",
        "MI": "샤오미코리아 공급사",
    }

    return mapping.get(simple, f"{brand} 코리아 공급사")


def random_phone():
    return f"02-{random.randint(100,999)}-{random.randint(1000,9999)}"

def random_address():
    cities = ["서울시 강남구", "서울시 송파구", "서울시 마포구", "경기도 성남시", "경기도 용인시", "인천시 부평구"]
    streets = ["테헤란로", "송파대로", "신촌로", "광화문로", "정자일로", "백현로"]
    return f"{random.choice(cities)} {random.choice(streets)} {random.randint(1,200)}"


def main():
    df = pd.read_csv(SKU_CSV)


    df["brand"] = df["product_code"].apply(lambda x: x.split("-")[1])
    all_brands = sorted(df["brand"].unique())

    print(f"[INFO] 총 브랜드 수 = {len(all_brands)}종")


    suppliers = []
    supplier_map = {}
    supplier_start_id = 600000

    for idx, brand in enumerate(all_brands):
        sid = supplier_start_id + idx
        supplier_map[brand] = sid

        suppliers.append({
            "supplier_id": sid,
            "supplier_code": f"SUP-{brand}",
            "supplier_name": korean_supplier_name(brand),
            "contact_name": random.choice(KOREAN_NAMES),
            "contact_number": random_phone(),
            "address": random_address(),
            "created_at": datetime.now(),
            "updated_at": datetime.now(),
        })

    supplier_df = pd.DataFrame(suppliers)
    supplier_df.to_csv(SUPPLIER_OUT, index=False, encoding="utf-8-sig")
    print(f"[OK] 공급사 CSV 생성 → {SUPPLIER_OUT}")


    product_sup_list = []

    for _, row in df.iterrows():
        brand = row["brand"]
        pid = int(row["product_id"])
        sid = supplier_map[brand]

        product_sup_list.append({
            "product_supplier_id": 650000 + pid,
            "product_id": pid,
            "supplier_id": sid,
            "supplier_product_code": f"{sid}-{row['product_code']}",
            "purchase_price": float(row["price"]) * 0.6,  # 공급가 = 소비자가 60%
            "lead_time_days": 2,
            "created_at": datetime.now(),
        })

    product_supplier_df = pd.DataFrame(product_sup_list)
    product_supplier_df.to_csv(PRODUCT_SUPPLIER_OUT, index=False, encoding="utf-8-sig")
    print(f"[OK] product_supplier CSV 생성 → {PRODUCT_SUPPLIER_OUT}")


if __name__ == "__main__":
    main()
