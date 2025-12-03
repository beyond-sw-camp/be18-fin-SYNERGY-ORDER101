# 1) 가벼운 Python 베이스 이미지 사용
FROM python:3.13-slim

# 2) 필수 OS 패키지 설치
# (pandas, numpy, lightgbm, sklearn 빌드 위해 필요)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        gcc \
        g++ \
        make \
        libgomp1 \
        libstdc++6 \
        libffi-dev \
        libssl-dev \
        build-essential \
        curl \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# 3) 작업 디렉토리 설정
WORKDIR /app

# 4) Poetry 설치
RUN pip install --upgrade pip && \
    pip install poetry

# 5) pyproject.toml / poetry.lock만 복사 (의존성 캐싱)
COPY pyproject.toml poetry.lock* /app/

# 6) Poetry 설정 (가상환경 만들지 않음)
RUN poetry config virtualenvs.create false

# 7) 의존성 설치 (dev dependency 제외)
RUN poetry install --without dev --no-root

# 8) 실제 앱 소스 복사
COPY . /app

# 9) 컨테이너 실행 시 FastAPI 서버 실행
EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
