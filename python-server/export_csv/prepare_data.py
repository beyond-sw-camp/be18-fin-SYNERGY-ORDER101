import pandas as pd
import random
import os
from datetime import datetime

# ------------------------------
# 기본 설정
# ------------------------------
BASE_PK = 600000
NOW = "2025-01-01 00:00:00"
os.makedirs("db_ready", exist_ok=True)

# ------------------------------
# 1) 카테고리 자동 생성
# ------------------------------
category_rows = [
    # 대분류
    (1, "대", "가전", None),
    (2, "대", "주방", None),
    (3, "대", "IT기기", None),

    # 중분류
    (4, "중", "영상가전", 1),
    (5, "중", "생활가전", 1),
    (6, "중", "계절가전", 1),
    (7, "중", "주방가전", 2),
    (8, "중", "컴퓨터", 3),
    (9, "중", "모바일", 3),

    # 소분류
    (10, "소", "TV", 4),
    (11, "소", "냉장고", 5),
    (12, "소", "세탁기", 5),
    (13, "소", "건조기", 5),
    (14, "소", "청소기", 5),
    (15, "소", "에어컨", 6),
    (16, "소", "공기청정기", 6),
    (17, "소", "전자레인지", 7),
    (18, "소", "오븐", 7),
    (19, "소", "식기세척기", 7),
    (20, "소", "커피머신", 7),
    (21, "소", "토스터기", 7),
    (22, "소", "믹서기", 7),
    (23, "소", "노트북", 8),
    (24, "소", "데스크탑", 8),
    (25, "소", "모니터", 8),
    (26, "소", "스마트폰", 9),
    (27, "소", "스마트워치", 9),
    (28, "소", "프린터", 8),
    (29, "소", "라우터", 8),
]

category_df = pd.DataFrame([{
    "product_category_id": cid,
    "category_level": level,
    "category_name": name,
    "created_at": NOW,
    "parent_category_id": parent
} for cid, level, name, parent in category_rows])

category_df.to_csv("db_ready/product_category.csv", index=False)
print("✔ product_category.csv 생성 완료")

# ------------------------------
# 2) 공급사 생성
# ------------------------------
supplier_rows = []
supplier_ids = []

for i in range(5):
    sid = BASE_PK + i
    supplier_ids.append(sid)

    supplier_rows.append({
        "supplier_id": sid,
        "supplier_code": f"SUP{i+1}",
        "supplier_name": f"공급사{i+1}",
        "contact_name": f"담당자{i+1}",
        "contact_number": "010-1234-5678",
        "address": "서울시 강남구",
        "created_at": NOW,
        "updated_at": NOW
    })

supplier_df = pd.DataFrame(supplier_rows)
supplier_df.to_csv("db_ready/supplier.csv", index=False)
print("✔ supplier.csv 생성 완료")

# ------------------------------
# 3) 스토어 / 창고 생성
# ------------------------------
store_df = pd.DataFrame([{
    "store_id": 1,
    "store_code": "ST001",
    "store_name": "강남점",
    "address": "서울 강남구",
    "contact_number": "02-1111-2222",
    "default_warehouse_id": 1,
    "is_active": 1,
    "created_at": NOW,
    "updated_at": NOW
}])

store_df.to_csv("db_ready/store.csv", index=False)
print("✔ store.csv 생성 완료")

warehouse_df = pd.DataFrame([{
    "warehouse_id": 1,
    "warehouse_code": "WH001",
    "warehouse_name": "본사창고",
    "address": "서울 서초구",
    "contact_number": "02-3333-4444",
    "is_active": 1,
    "created_at": NOW,
    "updated_at": NOW
}])

warehouse_df.to_csv("db_ready/warehouse.csv", index=False)
print("✔ warehouse.csv 생성 완료")

# ------------------------------
# 4) 상품 + 상품-공급사
# ------------------------------
sales = pd.read_csv("app/data_pipeline/domain_sales.csv")
unique_products = sales.groupby("product_id").agg({
    "product_name": "first",
    "cat_low": "first"
}).reset_index()

PRICE_MAP = {
    "TV": (500000, 2500000),
    "냉장고": (600000, 3000000),
    "세탁기": (500000, 1800000),
    "건조기": (600000, 1600000),
    "청소기": (100000, 600000),
    "에어컨": (400000, 2500000),
    "공기청정기": (100000, 500000),
    "전자레인지": (50000, 250000),
    "오븐": (100000, 600000),
    "식기세척기": (300000, 1200000),
}

def random_price(cat):
    low, high = PRICE_MAP.get(cat, (30000, 300000))
    return random.randint(low, high)

product_rows = []
product_supplier_rows = []
pid_map = {}
pk_counter = BASE_PK + 1000

for _, row in unique_products.iterrows():
    sku = row["product_id"]
    name = row["product_name"]
    cat = row["cat_low"]

    pid = pk_counter
    pid_map[sku] = pid

    product_rows.append({
        "product_id": pid,
        "product_category_id": 10,  # TV 임시
        "product_code": sku,
        "product_name": name,
        "image_url": "",
        "description": "",
        "status": 1,
        "created_at": NOW,
        "updated_at": NOW,
        "price": random_price(cat)
    })

    # 공급사 랜덤 배정
    sid = random.choice(supplier_ids)

    product_supplier_rows.append({
        "product_supplier_id": pk_counter + 50000,
        "product_id": pid,
        "supplier_id": sid,
        "supplier_product_code": f"{sid}-{sku}",
        "purchase_price": 0,
        "lead_time_days": 2,
        "created_at": NOW
    })

    pk_counter += 1

pd.DataFrame(product_rows).to_csv("db_ready/product.csv", index=False)
pd.DataFrame(product_supplier_rows).to_csv("db_ready/product_supplier.csv", index=False)
print("✔ product.csv / product_supplier.csv 생성 완료")

# ------------------------------
# 5) 재고 생성
# ------------------------------
store_inv = []
wh_inv = []

pk_counter = BASE_PK + 2000

for sku, pid in pid_map.items():
    store_inv.append({
        "store_inventory_id": pk_counter,
        "store_id": 1,
        "product_id2": pid,
        "on_hand_qty": 5,
        "in_transit_qty": 1,
        "updated_at": NOW
    })

    wh_inv.append({
        "warehouse_inventory_id": pk_counter,
        "warehouse_id": 1,
        "product_id": pid,
        "on_hand_qty": 50,
        "safety_qty": 10,
        "updated_at": NOW
    })

    pk_counter += 1

pd.DataFrame(store_inv).to_csv("db_ready/store_inventory.csv", index=False)
pd.DataFrame(wh_inv).to_csv("db_ready/warehouse_inventory.csv", index=False)
print("✔ store_inventory.csv / warehouse_inventory.csv 생성 완료")

# ------------------------------
# 6) demand_forecast 생성
# ------------------------------
df_rows = []
pk_counter = BASE_PK + 3000

for _, row in sales.iterrows():
    pid = pid_map[row["product_id"]]

    df_rows.append({
        "demand_forecast_id": pk_counter,
        "warehouse_id": row["warehouse_id"],
        "store_id": row["store_id"],
        "product_id": pid,
        "target_week": row["target_date"],
        "y_pred": 0,
        "actual_order_qty": row["actual_order_qty"],
        "mape": 0,
        "snapshot_at": NOW,
        "updated_at": NOW
    })

    pk_counter += 1

pd.DataFrame(df_rows).to_csv("db_ready/demand_forecast.csv", index=False)
print("✔ demand_forecast.csv 생성 완료")

# ------------------------------
# 7) 관리자(user) 1명 생성
# ------------------------------
user = pd.DataFrame([{
    "user_id": 1,
    "store_id": 1,
    "email": "admin@test.com",
    "password": "test1234",
    "name": "관리자",
    "role": "HQ_ADMIN",
    "is_active": 1,
    "created_at": NOW,
    "updated_at": NOW,
    "is_deleted": 0,
    "phone": "010-1111-2222"
}])

user.to_csv("db_ready/user.csv", index=False)
print("✔ user.csv 생성 완료")

print("\n🎉 모든 DB Seed CSV 생성 완료!")
