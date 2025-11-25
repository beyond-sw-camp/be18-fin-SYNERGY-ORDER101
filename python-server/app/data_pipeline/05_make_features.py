from pathlib import Path
import pandas as pd
import numpy as np

BASE = Path(__file__).resolve().parent
DATA = BASE / "domain_sales_sku.csv"
EXT  = BASE / "external_factors.csv"

OUT_ALL = BASE / "features_all.csv"
OUT_TR  = BASE / "features_train.csv"
OUT_TE  = BASE / "features_test.csv"

FORECAST_FREQ = "W-MON"
LAGS = [1,2,4,8,12]
MAS  = [4,8,12]
TEST_WEEKS = 52
MIN_HISTORY_WEEKS = 1   

def add_time_features(df):
    dt = pd.to_datetime(df["target_date"])
    df["year"] = dt.dt.year
    df["weekofyear"] = dt.dt.isocalendar().week.astype(int)
    df["month"] = dt.dt.month
    df["sin_week"] = np.sin(2*np.pi*df["weekofyear"] / 52)
    df["cos_week"] = np.cos(2*np.pi*df["weekofyear"] / 52)
    return df

def _to_week_start_monday(dt):
    dt = pd.to_datetime(dt)
    return (dt - pd.to_timedelta((dt.dt.weekday) % 7, unit="D")).dt.normalize()

def main():

    if not DATA.exists():
        raise FileNotFoundError(DATA)

    df = pd.read_csv(DATA, parse_dates=["target_date"], low_memory=False)

    if "cat_low" not in df.columns:
        df["cat_low"] = ""

    df["warehouse_id"] = pd.to_numeric(df["warehouse_id"], errors="coerce").astype("Int64")
    df["store_id"]     = pd.to_numeric(df["store_id"], errors="coerce").astype("Int64")
    df["sku_id"]       = df["sku_id"].astype(str)

    if "region" not in df.columns:
        df["region"] = "본사창고"

    df["actual_order_qty"] = pd.to_numeric(
        df["sku_qty"] if "sku_qty" in df.columns else df["actual_order_qty"],
        errors="coerce"
    ).fillna(0).astype(int)

    keys = ["warehouse_id","store_id","sku_id","region"]

    use = df[["target_date", *keys, "cat_low", "actual_order_qty"]].copy()
    use["target_date"] = _to_week_start_monday(use["target_date"])

    frames = []
    for grp, g in use.groupby(keys):
        cat_val = g["cat_low"].iloc[0]

        g_res = (
            g.set_index("target_date")
             .sort_index()
             .resample(FORECAST_FREQ)
             .sum()
        )

        for i, k in enumerate(keys):
            g_res[k] = grp[i]

        g_res["cat_low"] = cat_val
        frames.append(g_res.reset_index())

    dfw = pd.concat(frames, ignore_index=True)

    # ---------- lag / MA ----------
    dfw = dfw.sort_values(keys + ["target_date"])
    for lag in LAGS:
        dfw[f"lag_{lag}"] = dfw.groupby(keys)["actual_order_qty"].shift(lag)

    for ma in MAS:
        dfw[f"ma_{ma}"] = (
            dfw.groupby(keys)["actual_order_qty"]
               .transform(lambda x: x.shift(1).rolling(ma, min_periods=1).mean())
        )

    # ---------- share_norm / promo ----------
    if "share_norm" in df.columns:
        tmp = (
            df[["target_date", *keys, "share_norm"]]
              .assign(target_date=_to_week_start_monday(df["target_date"]))
              .groupby(["target_date", *keys], as_index=False)["share_norm"].mean()
        )
        dfw = dfw.merge(tmp, on=["target_date", *keys], how="left")
        dfw["share_norm"] = dfw["share_norm"].fillna(0)
        dfw["promo_flag"] = (dfw["share_norm"] > 0.25).astype(int)
    else:
        dfw["promo_flag"] = 0

    dfw["promo_flag_prev"] = (
        dfw.groupby(keys)["promo_flag"]
           .shift(1)
           .fillna(0)
           .astype(int)
    )

    dfw = add_time_features(dfw)

    # ---------- 외부 요인 ----------
    if EXT.exists():
        ext = pd.read_csv(EXT, parse_dates=["target_date"])
        if "region" not in ext.columns:
            ext["region"] = "본사창고"
        ext["target_date"] = _to_week_start_monday(ext["target_date"])
        dfw = dfw.merge(ext, on=["region","target_date"], how="left")

        for c in ext.columns:
            if c in ("region","target_date"):
                continue
            dfw[c] = dfw[c].fillna(0)

    dfw["y"] = dfw["actual_order_qty"].astype(float)

    # ---------- 최소 히스토리 ----------
    cnt = dfw.groupby(keys)["y"].transform("count")
    dfw = dfw[cnt >= MIN_HISTORY_WEEKS].copy()

    # ---------- Train / Test split ----------
    dfw = dfw.sort_values(keys + ["target_date"]).reset_index(drop=True)

    g = dfw.groupby(keys)
    pos = g.cumcount()
    size = g["target_date"].transform("size")

    is_last_test = (size > TEST_WEEKS) & (pos >= (size - TEST_WEEKS))
    dfw["split"] = np.where(is_last_test, "test", "train")

    dfw.to_csv(OUT_ALL, index=False)
    dfw[dfw["split"]=="train"].to_csv(OUT_TR, index=False)
    dfw[dfw["split"]=="test"].to_csv(OUT_TE, index=False)

    print(f"saved: {OUT_ALL.name} ({len(dfw):,})")

if __name__ == "__main__":
    main()
