"""
02_expand_sku_from_category.py  (time-varying shares + lifecycle + seasonality)
- 입력: cleaned_domain_sales.csv (없으면 domain_sales.csv), sku_catalog.csv
- 핵심:
  * 카테고리 주간 수요를 SKU별 시간 가변 점유율로 분해
  * base_share 주변 랜덤워크
  * SKU lifecycle(출시, 램프업, 감쇠)
  * 카테고리별 시즌 패턴 + SKU 속성 기반 선호도
  * 프로모션(랜덤 + promotions.csv) 효과
- 출력: domain_sales_sku.csv, sample_domain_sales_sku.csv
"""

from pathlib import Path
import pandas as pd
import numpy as np
import re

# ---------------------- PATH & 설정 ---------------------- #

BASE = Path(__file__).resolve().parent
DOMAIN_CLEAN = BASE / "cleaned_domain_sales.csv"
DOMAIN_RAW   = BASE / "domain_sales.csv"
SKU_CAT      = BASE / "sku_catalog.csv"
PROMO        = BASE / "promotions.csv"   # optional: sku_id,target_date,boost

OUT_FULL   = BASE / "domain_sales_sku.csv"
OUT_SAMPLE = BASE / "sample_domain_sales_sku.csv"

# 랜덤 시드
GLOBAL_SEED = 42

# 랜덤워크 & 노이즈
NOISE_SCALE = 0.03           # 주별 미세 잡음
RHO = 0.9                    # 점유율 랜덤워크 모멘텀(0.8~0.95)
RW_SIGMA = 0.12              # 랜덤워크 분산(로그공간, 살짝↑)

# Lifecycle
RAMP_WEEKS = 10              # 출시 후 램프업
DECAY_HALF_LIFE = 90         # 절반감쇠 주수

# 프로모션
PROMO_RATE = 0.05            # 랜덤 프로모션 발생 확률
PROMO_STRENGTH = 0.25        # 랜덤 프로모션 승수
DEFAULT_PROMO_BOOST = 0.20   # promotions.csv boost 기본값

# 기타
ID_PATTERN = r"^[A-Z0-9\-]+$"
MISC_KEYS = {
    "", "기타", "기타가전", "기타가전류", "기타제품",
    "etc", "misc", "others", "other"
}

# 카테고리 이름 heuristic (cat_low_norm 기준)
AC_KEYS       = {"aircon", "에어컨", "ac"}
DRYER_KEYS    = {"건조기", "dryer"}
PURIFIER_KEYS = {"공기청정기", "airpurifier", "aircleaner"}
TV_KEYS       = {"tv", "oled", "qled"}
WASHER_KEYS   = {"세탁기", "washingmachine", "washer"}


# ---------------------- Helper 함수 ---------------------- #

def _normalize_sum(x: np.ndarray) -> np.ndarray:
    s = x.sum()
    return x / s if s > 0 else x


def _norm_cat(x: str) -> str:
    if pd.isna(x):
        return ""
    x = str(x).strip()
    x = re.sub(r"\s+", "", x)
    x = x.replace("_", "").replace("-", "")
    return x.lower()


def _load_domain() -> pd.DataFrame:
    """
    cleaned_domain_sales.csv / domain_sales.csv 를 안전하게 로드.
    - engine="python", on_bad_lines="skip" 로 최대한 살려 읽기
    """
    path = DOMAIN_CLEAN if DOMAIN_CLEAN.exists() else DOMAIN_RAW
    print(f"[LOAD] reading {path} ...")

    df = pd.read_csv(
        path,
        parse_dates=["target_date"],
        engine="python",
        on_bad_lines="skip",
    )

    # 날짜 상한 (안전용)
    df = df[df["target_date"] <= "2025-12-31"].copy()

    # 타입 정리
    if "warehouse_id" in df.columns:
        df["warehouse_id"] = pd.to_numeric(df["warehouse_id"], errors="coerce").astype("Int64")
    else:
        df["warehouse_id"] = pd.NA

    if "store_id" in df.columns:
        df["store_id"] = pd.to_numeric(df["store_id"], errors="coerce").astype("Int64")
    else:
        df["store_id"] = pd.NA

    if "product_id" in df.columns:
        df["product_id"] = df["product_id"].astype("string")

    for col in ["product_name", "cat_top", "cat_mid", "cat_low", "region"]:
        if col in df.columns:
            df[col] = df[col].astype("string")

    if "actual_order_qty" in df.columns:
        df["actual_order_qty"] = (
            pd.to_numeric(df["actual_order_qty"], errors="coerce")
              .fillna(0)
              .astype(int)
        )

    print(f"Loaded {path.name}: {len(df):,} rows (after cleaning)")
    return df


def _load_catalog() -> pd.DataFrame:
    sku = pd.read_csv(SKU_CAT, dtype=str, low_memory=False)
    sku.columns = (
        sku.columns.astype(str)
           .str.replace("\ufeff", "", regex=False)
           .str.strip()
    )

    for col in [
        "sku_id","sku_name_en","sku_name_ko","cat_low","brand","series","model_code",
        "size_inch","volume_l","capacity_text","energy_grade","price_tier",
        "msrp_krw","launch_date","warranty_months","case_pack","min_order_qty",
        "eol_flag","base_share",
    ]:
        if col not in sku.columns:
            sku[col] = pd.NA

    # numeric/date cast
    for col in ["msrp_krw", "warranty_months", "case_pack", "min_order_qty", "eol_flag"]:
        sku[col] = pd.to_numeric(sku[col], errors="coerce")
    sku["launch_date"] = pd.to_datetime(sku["launch_date"], errors="coerce")
    sku["base_share"]  = pd.to_numeric(sku["base_share"], errors="coerce").fillna(0.0)

    for col in [
        "sku_id","sku_name_en","sku_name_ko","cat_low",
        "brand","series","model_code","size_inch","volume_l",
        "capacity_text","energy_grade","price_tier",
    ]:
        sku[col] = sku[col].astype("string").str.strip()

    return sku


def _load_promo() -> pd.DataFrame:
    if PROMO.exists():
        p = pd.read_csv(PROMO, parse_dates=["target_date"])
        if "boost" not in p.columns:
            p["boost"] = DEFAULT_PROMO_BOOST
        return p
    return pd.DataFrame(columns=["sku_id", "target_date", "boost"])


def _validate_sku_ids(sku: pd.DataFrame):
    bad = sku[~sku["sku_id"].astype(str).str.match(ID_PATTERN, na=False)]
    if len(bad) > 0:
        raise ValueError(
            f"sku_id 형식 오류: 예시 {bad.head(5)['sku_id'].tolist()} ... 전체 {len(bad)}건"
        )


def _life_multiplier(launch_date, t: pd.Timestamp) -> float:
    """출시 전 0, 이후 RAMP로 상승, 장기적으로 지수감쇠"""
    if pd.isna(launch_date):
        return 1.0
    weeks = int((t - launch_date).days // 7)
    if weeks < 0:
        return 0.0
    ramp = min(1.0, weeks / max(1, RAMP_WEEKS))
    lam = np.log(2) / max(1, DECAY_HALF_LIFE)
    decay = np.exp(-lam * max(0, weeks - RAMP_WEEKS))
    return ramp * decay + (1 - ramp) * 0.1


# ------------ 시즌/선호도 관련 Helper ------------ #

def _week_of_year(dt: pd.Timestamp) -> int:
    return int(dt.isocalendar().week)


def _cat_season_index(cat_norm: str, week: int) -> float:
    """
    카테고리별 시즌 인덱스 [0,1] 정도.
    - 에어컨: 여름(25~34)에 peak
    - 건조기/세탁기: 겨울/장마(1~8, 40~50) 약간↑
    - 공기청정기: 봄(미세먼지, 9~20) + 겨울(45~52)
    - TV: 연말(47~52) + 3월(학기 시작) 약간↑
    - 기타: 약한 flat seasonality
    """
    w = int(week)
    c = cat_norm

    def gauss(center, width):
        return np.exp(-0.5 * ((w - center) / width) ** 2)

    # 에어컨 계열
    if any(k in c for k in AC_KEYS):
        return gauss(28, 6)  # 여름 peak

    # 건조기 / 세탁기
    if any(k in c for k in DRYER_KEYS.union(WASHER_KEYS)):
        return max(gauss(5, 4), gauss(44, 6)) * 0.8 + 0.1

    # 공기청정기
    if any(k in c for k in PURIFIER_KEYS):
        s1 = gauss(13, 5)   # 봄
        s2 = gauss(48, 5)   # 겨울
        return max(s1, s2)

    # TV / AV
    if any(k in c for k in TV_KEYS):
        s1 = gauss(11, 3)   # 3월
        s2 = gauss(50, 3)   # 연말
        return max(s1, s2) * 0.8 + 0.1

    # 기타: 살짝만 요동
    return 0.2 + 0.1 * np.sin(2 * np.pi * w / 52)


def _safe_float(val) -> float:
    """pd.NA / None / '' 등을 안전하게 float로 변환 (실패 시 np.nan)."""
    if pd.isna(val):
        return np.nan
    s = str(val).strip().replace('"', '')
    if s == "":
        return np.nan
    try:
        return float(s)
    except ValueError:
        return np.nan


def _sku_attr_preference(sku_row: pd.Series, cat_norm: str) -> float:
    """
    SKU 속성 기반 '시즌 선호도 계수' (대략 -1 ~ +1)
    - 에어컨: 크기/에너지등급/프리미엄 가격 반영
    - 그 외: 가격대, 에너지등급을 이용한 작은 차이
    """
    price_tier = str(sku_row.get("price_tier") or "").lower()
    msrp = sku_row.get("msrp_krw")
    energy = str(sku_row.get("energy_grade") or "")

    size = _safe_float(sku_row.get("size_inch"))

    pref = 0.0

    # 프리미엄/저가
    if price_tier == "high":
        pref += 0.3
    elif price_tier == "low":
        pref -= 0.2

    # 에너지 1등급이면 여름/겨울에 좀 더 선호
    if "1" in energy:
        pref += 0.2

    # 에어컨/TV 에서 인치가 크면 약간 더 선호
    if not np.isnan(size) and any(k in cat_norm for k in AC_KEYS.union(TV_KEYS)):
        if size >= 65:
            pref += 0.3
        elif size >= 55:
            pref += 0.15

    # 너무 큰 값은 잘라냄
    return float(np.clip(pref, -0.8, 0.8))


# ------------ 시계열 분해: 시간 가변 점유율 생성 ------------ #

def _time_varying_shares(
    one_cat_week_df: pd.DataFrame,
    sku_meta: pd.DataFrame,
    cat_norm: str,
    promo_df: pd.DataFrame,
    rng: np.random.Generator
) -> pd.DataFrame:
    """
    카테고리 주간합 → SKU별 시간 가변 점유율로 분배
    - base_share (정적)
    - + 랜덤워크
    - + lifecycle
    - + 시즌 인덱스 * SKU 속성 선호도
    - + 프로모션(랜덤 + csv)
    """

    g = one_cat_week_df.sort_values("target_date").copy()
    sku_rows = sku_meta.reset_index(drop=True)
    n = len(sku_rows)

    if n == 0 or len(g) == 0:
        return pd.DataFrame(columns=[
            "target_date", "sku_id", "share_norm", "actual_order_qty", "sku_qty"
        ])

    # base_share 보정
    base = sku_rows["base_share"].to_numpy().clip(1e-6, 1.0)
    base = base / base.sum()
    logits = np.log(base)

    # SKU별 고정 선호도
    attr_pref = np.array([
        _sku_attr_preference(sku_rows.loc[i], cat_norm) for i in range(n)
    ])  # -1~+1 정도

    # 랜덤 프로모션 일정
    random_promos = rng.random((len(g), n)) < PROMO_RATE

    rows = []
    for t_idx, dt in enumerate(g["target_date"]):
        week = _week_of_year(dt)
        season_idx = _cat_season_index(cat_norm, week)  # 0~1

        # ---- 랜덤워크 ---- #
        logits = (
            RHO * logits
            + (1 - RHO) * np.log(base)
            + rng.normal(0, RW_SIGMA, size=n)
        )
        s = np.exp(logits - np.max(logits))
        s /= s.sum()

        # ---- 미세 노이즈 ---- #
        s = s * (1 + (rng.random(n) - 0.5) * 2 * NOISE_SCALE)

        # ---- 시즌 + SKU 선호도 적용 ---- #
        # season_idx (0~1) * attr_pref(-1~1) → 대략 ±35% 정도 영향
        season_effect = 1.0 + 0.35 * season_idx * attr_pref
        s = s * season_effect

        # ---- 프로모션(랜덤 + csv) ---- #
        # 1) 랜덤 프로모션
        promo_mult = np.where(random_promos[t_idx], 1.0 + PROMO_STRENGTH, 1.0)

        # 2) promotions.csv에 정의된 프로모션
        if not promo_df.empty:
            this_day = promo_df[promo_df["target_date"] == dt]
            if not this_day.empty:
                for _, row in this_day.iterrows():
                    sku_id = row["sku_id"]
                    boost  = float(row.get("boost", DEFAULT_PROMO_BOOST) or 0.0)
                    idx = np.where(sku_rows["sku_id"].to_numpy() == sku_id)[0]
                    if len(idx) > 0:
                        promo_mult[idx[0]] *= (1.0 + boost)

        s = s * promo_mult

        # ---- lifecycle 적용 ---- #
        life = np.array([
            _life_multiplier(sku_rows.loc[i, "launch_date"], dt) for i in range(n)
        ])
        s = s * life

        # ---- 정규화 ---- #
        if s.sum() <= 0:
            s = base.copy()
        s = _normalize_sum(s)

        rows.append(pd.DataFrame({
            "target_date": dt,
            "sku_id": sku_rows["sku_id"].tolist(),
            "share_norm": s,
        }))

    rep = pd.concat(rows, ignore_index=True)
    rep = rep.merge(
        g[["target_date", "actual_order_qty"]],
        on="target_date",
        how="left"
    )
    rep["sku_qty"] = (rep["actual_order_qty"] * rep["share_norm"]).round(0).astype(int)
    return rep


# ---------------------- main ---------------------- #

def main():
    rng = np.random.default_rng(GLOBAL_SEED)

    df    = _load_domain()
    sku   = _load_catalog()
    promo = _load_promo()
    _validate_sku_ids(sku)

    # 1) 카테고리 표준화 + 기타 제거
    sku["cat_low_norm"] = sku["cat_low"].apply(_norm_cat)
    df["cat_low_norm"]  = df["cat_low"].apply(_norm_cat)

    allowed = set(sku["cat_low_norm"].dropna().tolist())
    keep = df["cat_low_norm"].isin(allowed) & (~df["cat_low_norm"].isin(MISC_KEYS))
    if (len(df) - keep.sum()) > 0:
        print(f"Dropped by whitelist: {len(df)-keep.sum():,}/{len(df):,}")
    df = df[keep].copy()

    # 2) base_share 정규화(그룹합=1, 합=0이면 균등)
    pos_sum = sku.groupby("cat_low_norm")["base_share"].transform("sum")
    mask_pos = pos_sum > 1e-12
    sku.loc[mask_pos, "base_share"] = sku.loc[mask_pos, "base_share"] / pos_sum[mask_pos]
    zero_keys = sku.loc[~mask_pos, "cat_low_norm"].dropna().unique().tolist()
    for key in zero_keys:
        idx = sku.index[sku["cat_low_norm"] == key]
        n = len(idx)
        if n > 0:
            sku.loc[idx, "base_share"] = 1.0 / n
    if zero_keys:
        print(f"base_share 합 0 → 균등분배: {sorted(zero_keys)}")

    # 3) 카테고리-주간 합
    keys = ["warehouse_id", "region", "store_id", "cat_low_norm", "target_date"]
    cat_week = df.groupby(keys, as_index=False)["actual_order_qty"].sum()

    # 4) 카탈로그 조인
    cat_map = cat_week.merge(
        sku.rename(columns={"cat_low_norm": "cat_low_norm_join"}),
        left_on="cat_low_norm",
        right_on="cat_low_norm_join",
        how="left",
        validate="many_to_many"
    )
    if cat_map["sku_id"].isna().any():
        missing = cat_map.loc[cat_map["sku_id"].isna(), "cat_low_norm"].unique().tolist()
        raise ValueError(f"카탈로그 누락 cat_low_norm: {missing}")

    # 5) 그룹별 시간가변 점유율 생성
    out_list = []
    for (wh, region, store, cat_norm), g_cat in cat_map.groupby(
        ["warehouse_id", "region", "store_id", "cat_low_norm"]
    ):
        sku_meta = (
            g_cat[[
                "sku_id","base_share","launch_date",
                "sku_name_en","sku_name_ko","brand","series","model_code",
                "size_inch","volume_l","capacity_text","energy_grade","price_tier",
                "msrp_krw","warranty_months","case_pack","min_order_qty","eol_flag"
            ]]
            .drop_duplicates("sku_id")
        )

        tv = _time_varying_shares(
            one_cat_week_df=g_cat[["target_date", "actual_order_qty"]].drop_duplicates(),
            sku_meta=sku_meta,
            cat_norm=cat_norm,
            promo_df=promo,
            rng=rng,
        )

        tv["warehouse_id"] = wh
        tv["region"]       = region
        tv["store_id"]     = store
        tv["cat_low"]      = g_cat["cat_low"].iloc[0]  # 원본 한글 카테고리 유지
        tv = tv.merge(sku_meta, on="sku_id", how="left")
        out_list.append(tv)

    out = pd.concat(out_list, ignore_index=True)
    out = out[[
        "warehouse_id","region","store_id","target_date","cat_low",
        "sku_id","sku_name_en","sku_name_ko","brand","series","model_code",
        "size_inch","volume_l","capacity_text","energy_grade","price_tier",
        "msrp_krw","launch_date","warranty_months","case_pack","min_order_qty","eol_flag",
        "actual_order_qty","share_norm","sku_qty",
    ]].sort_values(["store_id", "target_date", "cat_low", "sku_id"]).reset_index(drop=True)

    OUT_FULL.parent.mkdir(parents=True, exist_ok=True)
    out.to_csv(OUT_FULL, index=False)
    out.sample(min(2000, len(out)), random_state=GLOBAL_SEED).to_csv(OUT_SAMPLE, index=False)
    print(f"saved:\n - {OUT_FULL}\n - {OUT_SAMPLE}\nrows={len(out):,}")


if __name__ == "__main__":
    main()
