import random
import csv
from pathlib import Path
from datetime import datetime
from itertools import product
BASE = Path(__file__).resolve().parent
OUT = BASE / "sku_catalog.csv"

random.seed(42)


BRANDS = {
    "TV": ["Samsung", "LG"],
    "냉장고": ["Samsung", "LG", "Winia"],
    "세탁기": ["Samsung", "LG", "Winia"],
    "건조기": ["Samsung", "LG", "Winia"],
    "청소기": ["Dyson", "Samsung"],
    "에어컨": ["Samsung", "LG"],
    "공기청정기": ["Samsung", "Dyson"],
    "전자레인지": ["Samsung", "LG"],
    "오븐": ["Samsung", "Cuckoo"],
    "식기세척기": ["LG", "Samsung"],
    "커피머신": ["DolceGusto", "Nespresso"],
    "토스터기": ["Philips", "Cuchen"],
    "믹서기": ["Philips", "Tefal"],
    "노트북": ["Samsung", "LG", "Apple"],
    "데스크탑": ["Samsung", "Apple"],
    "모니터": ["Samsung", "Apple"],
    "프린터": ["Canon", "Epson"],
    "라우터": ["ASUS", "Netgear"],
    "스마트폰": ["Apple", "Samsung", "LG"],
    "스마트워치": ["Apple", "Samsung"],
}



CATEGORY_OPTIONS = {
    "TV": {
        "opt": {"size": ["55인치", "65인치", "75인치"],
                "resolution": ["4K", "8K"]},
        "price": (400000, 5000000)
    },
    "냉장고": {
        "opt": {"type": ["양문형", "일반형"],
                "capacity": ["300L", "400L"]},
        "price": (400000, 3000000)
    },
    "세탁기": {
        "opt": {"type": ["드럼", "통돌이"],
                "capacity": ["12kg", "15kg", "20kg"]},
        "price": (400000, 2500000)
    },
    "건조기": {
        "opt": {"capacity": ["9kg", "16kg"],
                "tech": ["히트펌프", "인버터"]},
        "price": (400000, 2500000)
    },
    "청소기": {
        "opt": {"type": ["스틱형", "유선형"],
                "power": ["강력", "표준"]},
        "price": (50000, 700000)
    },
    "에어컨": {
        "opt": {"type": ["스탠드형", "벽걸이형"],
                "capacity": ["18평", "23평"]},
        "price": (400000, 2500000)
    },
    "공기청정기": {
        "opt": {"area": ["10평", "15평"],
                "filter": ["HEPA", "초미세먼지"]},
        "price": (50000, 600000)
    },
    "전자레인지": {
        "opt": {"capacity": ["20L", "25L"],
                "type": ["광파", "전자식"]},
        "price": (30000, 500000)
    },
    "오븐": {
        "opt": {"capacity": ["20L", "30L"],
                "type": ["컨벡션", "스팀오븐"]},
        "price": (50000, 500000)
    },
    "식기세척기": {
        "opt": {"type": ["빌트인", "스탠드형"],
                "capacity": ["8인용", "12인용"]},
        "price": (400000, 1500000)
    },
    "커피머신": {
        "opt": {"type": ["캡슐", "전자동"],
                "feature": ["우유추출", "온도조절"]},
        "price": (30000, 300000)
    },
    "토스터기": {
        "opt": {"slot": ["1구", "2구"],
                "mode": ["해동", "굽기"]},
        "price": (30000, 200000)
    },
    "믹서기": {
        "opt": {"power": ["600W", "1000W"],
                "type": ["일반", "고속 블렌더"]},
        "price": (30000, 200000)
    },
    "노트북": {
        "opt": {"ram": ["8GB", "16GB"],
                "ssd": ["256GB", "512GB"],
                "size": ["14인치", "15인치"]},
        "price": (700000, 4000000)
    },
    "데스크탑": {
        "opt": {"cpu": ["i5", "i7"],
                "ram": ["16GB", "32GB"],
                "ssd": ["256GB", "512GB"]},
        "price": (500000, 3000000)
    },
    "모니터": {
        "opt": {"size": ["24인치", "27인치"],
                "panel": ["IPS", "OLED"]},
        "price": (100000, 1000000)
    },
    "프린터": {
        "opt": {"type": ["잉크젯", "레이저"],
                "feature": ["복사", "Fax"]},
        "price": (50000, 500000)
    },
    "라우터": {
        "opt": {"band": ["AX", "AC"],
                "antenna": ["2안테나", "4안테나"]},
        "price": (20000, 100000)
    },
    "스마트폰": {
        "opt": {"storage": ["128GB", "256GB",],
                "color": ["블랙", "화이트"]},
        "price": (150000, 2000000)
    },
    "스마트워치": {
        "opt": {"size": ["40mm", "44mm"],
                "band": ["실리콘"]},
        "price": (50000, 500000)
    },
}



def make_price(low, high):
    step = 10000
    low = low // step
    high = high // step
    return random.randint(low, high) * step

def normalize_brand(brand):
    return brand[:3].upper()

def slugify(txt):
    return txt.upper().replace(" ", "").replace("　", "")



def main():
    product_rows = []
    product_id = 1
    now = "2025-01-01 00:00:00"

    prefix_map = {
        "TV": "TV",
        "냉장고": "FR",
        "세탁기": "WM",
        "건조기": "DRY",
        "청소기": "VAC",
        "에어컨": "AC",
        "공기청정기": "AIR",
        "전자레인지": "MW",
        "오븐": "OV",
        "식기세척기": "DW",
        "커피머신": "COF",
        "토스터기": "TOA",
        "믹서기": "MIX",
        "노트북": "NBK",
        "데스크탑": "DES",
        "모니터": "MON",
        "프린터": "PRN",
        "라우터": "RTR",
        "스마트폰": "PHN",
        "스마트워치": "WAT",
    }

    for cat_name, config in CATEGORY_OPTIONS.items():
        brands = BRANDS[cat_name]
        options = config["opt"]
        price_range = config["price"]

        # 옵션 key 리스트
        opt_keys = list(options.keys())
        # 옵션 value 리스트
        opt_values = list(options.values())

        # 옵션 전체 조합 생성
        option_combinations = list(product(*opt_values))

        # 브랜드 × 옵션 전체 조합
        for brand in brands:
            for combo in option_combinations:

                chosen = dict(zip(opt_keys, combo))
                opt_str = " ".join(chosen.values())

                product_name = f"{brand} {cat_name} {opt_str}"
                brand_code = normalize_brand(brand)
                option_code = slugify("-".join(chosen.values()))
                code_prefix = prefix_map.get(cat_name, cat_name[:3])

                product_code = f"{code_prefix}-{brand_code}-{option_code}-{product_id:05d}"

                price = make_price(*price_range)

                product_rows.append([
                    product_id,
                    now,
                    "",
                    "",
                    price,
                    product_code,
                    product_name,
                    1,
                    now,
                    cat_name
                ])

                product_id += 1

 

    with open(OUT, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.writer(f)
        writer.writerow([
            "product_id", "created_at", "description",
            "image_url", "price", "product_code",
            "product_name", "status", "updated_at", "category_name"
        ])
        writer.writerows(product_rows)

    print(f"SKU Catalog 생성 완료 → {OUT}")
    print(f"총 SKU 개수: {len(product_rows):,}")


if __name__ == "__main__":
    main()
