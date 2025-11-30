import random
import csv
from pathlib import Path

BASE = Path(__file__).resolve().parent
OUT = BASE / "sku_catalog_ml.csv"   

random.seed(42)

# -----------------------------------------------------------
# 1) 브랜드 목록
# -----------------------------------------------------------

BRANDS = {
    "TV": ["Samsung", "LG", "Sony", "TCL", "Hisense", "Panasonic", "Xiaomi", "Vizio"],
    "냉장고": ["Samsung", "LG", "Winia", "Siemens", "Bosch", "Haier", "Panasonic", "Electrolux"],
    "세탁기": ["Samsung", "LG", "Winia", "Bosch", "Electrolux", "Siemens", "Panasonic"],
    "건조기": ["Samsung", "LG", "Winia", "Bosch", "Electrolux", "Miele", "AEG"],
    "청소기": ["Dyson", "Samsung", "LG", "Roborock", "Xiaomi", "Eufy", "iRobot", "Tefal"],
    "에어컨": ["Samsung", "LG", "Carrier", "Winia", "Haier", "Panasonic", "Toshiba"],
    "공기청정기": ["Samsung", "LG", "Dyson", "Xiaomi", "Cuckoo", "Carrier", "Philips", "Blueair"],
    "전자레인지": ["Samsung", "LG", "Winia", "Panasonic", "Sharp", "Cuckoo", "MagicChef"],
    "오븐": ["Samsung", "LG", "Cuckoo", "Cuchen", "Panasonic", "Breville", "Philips"],
    "식기세척기": ["LG", "Samsung", "Bosch", "Miele", "Electrolux", "AEG"],
    "커피머신": ["Breville", "DolceGusto", "Nespresso", "DeLonghi", "Philips", "Simeo", "Cuckoo"],
    "토스터기": ["Breville", "Philips", "Cuchen", "DeLonghi", "RussellHobbs", "Balmuda"],
    "믹서기": ["Braun", "Philips", "Tefal", "Vitamix", "Blendtec", "Cuckoo"],
    "노트북": ["Samsung", "LG", "Apple", "HP", "Dell", "Lenovo", "ASUS", "Acer", "MSI"],
    "데스크탑": ["Samsung", "LG", "HP", "Dell", "Lenovo", "ASUS", "MSI"],
    "모니터": ["Samsung", "LG", "Dell", "ASUS", "Acer", "BenQ", "MSI", "Gigabyte"],
    "프린터": ["HP", "Canon", "Brother", "Samsung", "Epson", "Xerox"],
    "라우터": ["ASUS", "Netgear", "TP-Link", "IPTIME", "Linksys", "Xiaomi"],
    "스마트폰": ["Apple", "Samsung", "Xiaomi", "OPPO", "Vivo", "Google", "Motorola"],
    "스마트워치": ["Apple", "Samsung", "Xiaomi", "Garmin", "Fitbit", "Huawei"],
}

# -----------------------------------------------------------
# 2) 카테고리 옵션 + 가격 범위(의미 있는 옵션명)
# -----------------------------------------------------------

CATEGORY_OPTIONS = {
    "TV": {
        "opt": {"size": ["43인치", "50인치", "55인치", "65인치", "75인치"],
                "resolution": ["FHD", "4K", "OLED"]},
        "price": (400000, 5000000)
    },
    "냉장고": {
        "opt": {"type": ["양문형", "비스포크", "일반형", "상냉장 하냉동"],
                "capacity": ["300L", "400L", "500L", "700L"]},
        "price": (400000, 3000000)
    },
    "세탁기": {
        "opt": {"type": ["드럼", "통돌이"],
                "capacity": ["12kg", "15kg", "20kg", "24kg"]},
        "price": (400000, 2500000)
    },
    "건조기": {
        "opt": {"capacity": ["9kg", "12kg", "16kg", "20kg"],
                "tech": ["히트펌프", "인버터"]},
        "price": (400000, 2500000)
    },
    "청소기": {
        "opt": {"type": ["스틱형", "로봇청소기", "유선형"],
                "power": ["강력", "표준"]},
        "price": (50000, 700000)
    },
    "에어컨": {
        "opt": {"type": ["스탠드형", "벽걸이형"],
                "capacity": ["18평", "23평", "30평"]},
        "price": (400000, 2500000)
    },
    "공기청정기": {
        "opt": {"area": ["10평", "15평", "20평", "30평"],
                "filter": ["HEPA", "초미세먼지"]},
        "price": (50000, 600000)
    },
    "전자레인지": {
        "opt": {"capacity": ["20L", "25L", "30L"],
                "type": ["광파", "전자식"]},
        "price": (30000, 500000)
    },
    "오븐": {
        "opt": {"capacity": ["20L", "30L", "40L"],
                "type": ["컨벡션", "스팀오븐"]},
        "price": (50000, 500000)
    },
    "식기세척기": {
        "opt": {"type": ["빌트인", "스탠드형"],
                "capacity": ["8인용", "12인용"]},
        "price": (400000, 1500000)
    },
    "커피머신": {
        "opt": {"type": ["캡슐", "반자동", "전자동"],
                "feature": ["우유추출", "온도조절"]},
        "price": (30000, 300000)
    },
    "토스터기": {
        "opt": {"slot": ["1구", "2구"],
                "mode": ["베이글", "해동", "굽기"]},
        "price": (30000, 200000)
    },
    "믹서기": {
        "opt": {"power": ["400W", "600W", "1000W"],
                "type": ["일반", "고속 블렌더"]},
        "price": (30000, 200000)
    },
    "노트북": {
        "opt": {"ram": ["8GB", "16GB", "32GB"],
                "ssd": ["256GB", "512GB", "1TB"],
                "size": ["13인치", "14인치", "15인치"]},
        "price": (700000, 4000000)
    },
    "데스크탑": {
        "opt": {"cpu": ["i5", "i7", "i9"],
                "ram": ["8GB", "16GB", "32GB"],
                "ssd": ["256GB", "512GB", "1TB"]},
        "price": (500000, 3000000)
    },
    "모니터": {
        "opt": {"size": ["24인치", "27인치", "32인치"],
                "panel": ["IPS", "VA", "OLED"]},
        "price": (100000, 1000000)
    },
    "프린터": {
        "opt": {"type": ["잉크젯", "레이저"],
                "feature": ["스캔", "복사", "Fax"]},
        "price": (50000, 500000)
    },
    "라우터": {
        "opt": {"band": ["AX", "AC", "N"],
                "antenna": ["2안테나", "4안테나"]},
        "price": (20000, 100000)
    },
    "스마트폰": {
        "opt": {"storage": ["64GB", "128GB", "256GB", "512GB"],
                "color": ["블랙", "화이트", "블루", "레드"]},
        "price": (150000, 2000000)
    },
    "스마트워치": {
        "opt": {"size": ["40mm", "44mm"],
                "band": ["실리콘", "메탈", "가죽"]},
        "price": (50000, 500000)
    },
}

# SKU 개수
MAJOR_COUNT = 200
MINOR_COUNT = 70

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
    rows = []
    sku_id = 1

    for cat_name, config in CATEGORY_OPTIONS.items():
        brands = BRANDS[cat_name]
        options = config["opt"]
        price_range = config["price"]

        for _ in range(MAJOR_COUNT + MINOR_COUNT):
            brand = random.choice(brands)

            chosen = {k: random.choice(v) for k, v in options.items()}
            opt_str = " ".join(chosen.values())

            product_name = f"{brand} {cat_name} {opt_str}"

            brand_code = normalize_brand(brand)
            opt_code = slugify("-".join(chosen.values()))
            product_code = f"{slugify(cat_name)[:2]}-{brand_code}-{opt_code}"

            price = make_price(*price_range)

            rows.append([
                sku_id,
                product_code,
                product_name,
                brand,
                opt_str,
                price,
                cat_name,
                0.0  # base_share placeholder
            ])

            sku_id += 1

    with open(OUT, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.writer(f)
        writer.writerow([
            "sku_id", "product_code", "sku_name_ko",
            "brand", "minor_option", "msrp_krw",
            "cat_low", "base_share"
        ])
        writer.writerows(rows)

    print(f"ML용 SKU Catalog 생성 완료 → {OUT}")
    print(f"총 SKU 개수: {len(rows):,}")


if __name__ == "__main__":
    main()
