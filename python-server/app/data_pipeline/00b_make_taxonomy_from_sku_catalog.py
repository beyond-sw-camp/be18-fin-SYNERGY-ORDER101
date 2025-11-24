from pathlib import Path
import pandas as pd

BASE = Path(__file__).resolve().parent
SRC = BASE / "sku_catalog.csv"
OUT = BASE / "taxonomy_product_map.csv"

def main():
    if not SRC.exists():
        raise FileNotFoundError(f"{SRC} not found")

    df = pd.read_csv(SRC, dtype=str, low_memory=False)
    print(f"Loaded sku_catalog.csv: {len(df):,} rows")

    need = ["sku_id", "sku_name_ko", "cat_low"]
    missing = [c for c in need if c not in df.columns]
    if missing:
        raise ValueError(f"Missing columns in sku_catalog.csv: {missing}")

    # taxonomy 형태로 변환
    tax = pd.DataFrame({
        "product_id": df["sku_id"],
        "product_name": df["sku_name_ko"],
        "cat_top": "",
        "cat_mid": "",
        "cat_low": df["cat_low"]
    })

    tax.to_csv(OUT, index=False, encoding="utf-8-sig")
    print(f"Saved taxonomy_product_map.csv ({len(tax):,} rows)")

if __name__ == "__main__":
    main()
