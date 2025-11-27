import pandas as pd
df = pd.read_csv("app/data_pipeline/domain_sales.csv")
print("unique SKU:", len(df["product_id"].unique()))
print(df["product_id"].unique()[:50])
