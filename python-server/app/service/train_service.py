def retrain_model() -> None:
    pass
    
    # TODO: 여기에서
    #   1) 과거 N년치 데이터 로딩 (DB or CSV)
    #   2) feature 가공
    #   3) LightGBM 모델 학습
    #   4) model 파일 저장 (예: models/lgbm_model.pkl)


    # 지금은 구조만 만들고 pass 
    
    # pseudo-code
    # df = load_training_data()
    # X_train, y_train = make_features(df)
    # model = lgb.LGBMRegressor(...)
    # model.fit(X_train, y_train)
    # joblib.dump(model, "models/demand_lgbm.pkl")