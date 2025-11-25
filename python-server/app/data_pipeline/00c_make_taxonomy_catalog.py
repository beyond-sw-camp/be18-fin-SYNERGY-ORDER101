from pathlib import Path
import pandas as pd

BASE = Path(__file__).resolve().parent
SKU  = BASE / "sku_catalog.csv"
OUT  = BASE / "taxonomy_product_map.csv"

# DB product_category 테이블 기준 매핑
#  - cat_top  : LARGE (가전제품 / 주방가전 / IT기기)
#  - cat_mid  : MEDIUM (영상가전 / 생활가전 / 계절가전 / 주방가전 / 소형가전 / 컴퓨터 / 모바일 / 주변기기)
CAT_MAP = {
    "TV": ("가전제품", "영상가전"),
    "냉장고": ("가전제품", "생활가전"),
    "세탁기": ("가전제품", "생활가전"),
    "건조기": ("가전제품", "생활가전"),
    "청소기": ("가전제품", "생활가전"),
    "에어컨": ("가전제품", "계절가전"),
    "공기청정기": ("가전제품", "계절가전"),

    "전자레인지": ("주방가전", "주방가전"),
    "오븐": ("주방가전", "주방가전"),
    "식기세척기": ("주방가전", "주방가전"),
    "커피머신": ("주방가전", "소형가전"),
    "토스터기": ("주방가전", "소형가전"),
    "믹서기": ("주방가전", "소형가전"),

    "노트북": ("IT기기", "컴퓨터"),
    "데스크탑": ("IT기기", "컴퓨터"),
    "모니터": ("IT기기", "컴퓨터"),
    "스마트폰": ("IT기기", "모바일"),
    "스마트워치": ("IT기기", "모바일"),
    "프린터": ("IT기기", "주변기기"),
    "라우터": ("IT기기", "주변기기"),
}

def main():
    if not SKU.exists():
        raise FileNotFoundError(f"{SKU} not found")

    df = pd.read_csv(SKU)
    print(f"Loaded sku_catalog.csv: {len(df):,} rows")
    print("columns:", df.columns.tolist())

    # 1) 기본 컬럼 체크
    for c in ["sku_id", "sku_name_ko", "cat_low"]:
        if c not in df.columns:
            raise KeyError(f"sku_catalog.csv 에 '{c}' 컬럼이 없습니다. 현재 컬럼: {list(df.columns)}")

    # 2) product_id / product_name 생성
    df["product_id"] = df["sku_id"]
    df["product_name"] = df["sku_name_ko"]

    # 3) 상/중분류 채우기
    df["cat_low"] = df["cat_low"].astype(str).str.strip()
    df["cat_top"] = ""
    df["cat_mid"] = ""

    for low, (top, mid) in CAT_MAP.items():
        mask = df["cat_low"] == low
        df.loc[mask, "cat_top"] = top
        df.loc[mask, "cat_mid"] = mid

    # 매핑 안 된 cat_low 값 있으면 경고 출력
    missing = df[df["cat_top"] == ""]
    if not missing.empty:
        print("⚠ 매핑 안 된 cat_low 값:", missing["cat_low"].unique())

    # 4) 최종 taxonomy 파일 저장
    out = df[["product_id", "product_name", "cat_top", "cat_mid", "cat_low"]]
    out.to_csv(OUT, index=False, encoding="utf-8-sig")
    print(f"saved taxonomy: {OUT} (rows={len(out):,})")

if __name__ == "__main__":
    main()
