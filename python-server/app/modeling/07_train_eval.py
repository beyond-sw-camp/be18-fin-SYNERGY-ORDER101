from pathlib import Path
import json
import numpy as np
import pandas as pd
from lightgbm import LGBMRegressor, early_stopping, log_evaluation
from sklearn.metrics import mean_absolute_error
import joblib

BASE = Path(__file__).resolve().parents[1] / "data_pipeline"
TR = BASE / "features_train.csv"
TE = BASE / "features_test.csv"
OUT_PRED    = BASE / "predictions.csv"
MODEL_PATH  = BASE / "lightgbm_model.pkl"
FEATS_JSON  = BASE / "lightgbm_features.json"
METRICS_CSV = BASE / "metrics_eval.csv"
FEATURE_IMPORTANCE_CSV = BASE / "feature_importance.csv" 

ID_KEYS = ["warehouse_id","store_id","sku_id","region","target_date","split"]
TARGET = "y"
VAL_WEEKS = 26
LEAKY = {"actual_order_qty", "share_norm", "promo_flag"}

def smape(y_true, y_pred):
    denom = (np.abs(y_true) + np.abs(y_pred)) / 2.0
    diff  = np.abs(y_true - y_pred)
    denom = np.where(denom==0, 1.0, denom)
    return np.mean(diff / denom) * 100.0

def mape(y_true, y_pred):
    denom = np.where(y_true==0, 1.0, y_true)
    return (np.abs((y_true - y_pred) / denom)).mean() * 100.0

def add_prev_flags(df: pd.DataFrame) -> pd.DataFrame:
    if "promo_flag" in df.columns:
        df["promo_flag_prev"] = (
            df.groupby(["warehouse_id","store_id","sku_id"])["promo_flag"]
              .shift(1).fillna(0)
        )
    return df

def build_features(df: pd.DataFrame) -> list:
    ignore = set(ID_KEYS + [TARGET, "actual_order_qty"])
    numeric_cols = set(df.select_dtypes(include=["number","bool"]).columns.tolist())
    feats = [c for c in df.columns if c in numeric_cols and c not in ignore and c not in LEAKY]
    if "promo_flag_prev" in df.columns and "promo_flag_prev" not in feats:
        feats.append("promo_flag_prev")
    feats = list(dict.fromkeys(feats))
    return feats

def drop_low_variance(df_train: pd.DataFrame, cols, nunique_thresh=1, var_thresh=1e-8):
    nun = df_train[cols].nunique()
    low_nun = nun[nun <= nunique_thresh].index.tolist()
    var = df_train[cols].var(numeric_only=True).fillna(0.0)
    low_var = var[var <= var_thresh].index.tolist()
    drop = sorted(set(low_nun + low_var))
    keep = [c for c in cols if c not in drop]
    return keep, drop

def main():
    print("DATA DIR:", BASE)
    tr = pd.read_csv(TR, parse_dates=["target_date"])
    te = pd.read_csv(TE, parse_dates=["target_date"])

    tr = add_prev_flags(tr); te = add_prev_flags(te)
    features = build_features(tr)

    for c in features:
        if tr[c].dtype.kind in "fi":
            tr[c] = tr[c].fillna(0.0); te[c] = te[c].fillna(0.0)
        else:
            tr[c] = tr[c].fillna(0);   te[c] = te[c].fillna(0)

    features, dropped = drop_low_variance(tr, features)
    print(f"[DEBUG] using {len(features)} features:", features)
    if dropped:
        print("dropped low-variance:", dropped)

    # 마지막 12주 검증 분리
    keys = ["warehouse_id","store_id","sku_id"]
    tr2 = tr.sort_values(keys + ["target_date"]).copy()
    tr2["idx_in_grp"] = tr2.groupby(keys).cumcount()
    tr2["grp_size"]   = tr2.groupby(keys)[TARGET].transform("size")
    tr2["is_val"]     = (tr2["grp_size"] - tr2["idx_in_grp"]) <= VAL_WEEKS
    tr_fit = tr2.loc[~tr2["is_val"]].drop(columns=["idx_in_grp","grp_size","is_val"])
    tr_val = tr2.loc[ tr2["is_val"]].drop(columns=["idx_in_grp","grp_size","is_val"])

    X_tr, y_tr = tr_fit[features], tr_fit[TARGET]
    X_va, y_va = tr_val[features], tr_val[TARGET]

    model = LGBMRegressor(
        objective="poisson",
        n_estimators=3000,
        learning_rate=0.02,
        num_leaves=31,    
        max_depth=-1,
        min_child_samples=128,  
        min_gain_to_split=0.05,  
        subsample=0.7,
        subsample_freq=1,
        colsample_bytree=0.6, 
        reg_alpha=5.0,         
        reg_lambda=5.0,   
        random_state=42,
        verbosity=-1,
    )
    model.fit(
        X_tr, y_tr,
        eval_set=[(X_va, y_va)],
        eval_metric="mae",
        callbacks=[early_stopping(200, first_metric_only=True), log_evaluation(200)]
    )
    best_iter = getattr(model, "best_iteration_", None)

    te_pred = np.clip(model.predict(te[features], num_iteration=best_iter), 0, None)
    mae   = mean_absolute_error(te[TARGET], te_pred)
    _mape = mape(te[TARGET].values, te_pred)
    _smape= smape(te[TARGET].values, te_pred)
    print(f"MAE={mae:,.3f} | MAPE={_mape:,.2f}% | SMAPE={_smape:,.2f}% (n={len(te)})")

    # feature importance 저장
    fi = pd.DataFrame({
        "feature": features,
        "importance": model.feature_importances_
    }).sort_values("importance", ascending=False)
    fi.to_csv(FEATURE_IMPORTANCE_CSV, index=False)
    print(f"saved feature importance: {FEATURE_IMPORTANCE_CSV}")


    keep_ids = [c for c in ["warehouse_id","store_id","sku_id","region","target_date"] if c in te.columns]
    out = te[keep_ids + [TARGET]].copy()
    out["y_pred"] = te_pred.round(0).astype(int)
    out.to_csv(OUT_PRED, index=False)
    print(f"saved: {OUT_PRED} ({len(out):,} rows)")

    joblib.dump(model, MODEL_PATH)
    with open(FEATS_JSON, "w", encoding="utf-8") as f:
        json.dump(features, f, ensure_ascii=False, indent=2)
    pd.DataFrame({"metric":["mae","mape","smape"],
                  "value":[mae, _mape, _smape],
                  "best_iteration":[best_iter]*3}).to_csv(METRICS_CSV, index=False)

    print(f"model saved to {MODEL_PATH}")
    print(f"best_iteration: {best_iter}")

if __name__ == "__main__":
    main()
