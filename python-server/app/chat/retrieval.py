import os
import json
import numpy as np
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity

BASE_DIR = "knowledge"

CATEGORY_LABELS = {
    "order": "발주",
    "warehouse_inventory_product": "창고/재고/상품",
    "franchise_delivery": "가맹점/배송",
    "ai": "AI",
    "system": "운영/시스템"
}

EMBED_MODEL = SentenceTransformer("intfloat/multilingual-e5-base")

class KnowledgeBase:
    def __init__(self):
        self.categories = {}
        self.documents = []      # [{category, question, answer}]
        self.doc_texts = []      # 질문만
        self.embeddings = None   # numpy array

    def load_all(self):
        for filename in os.listdir(BASE_DIR):
            if filename.endswith(".json"):
                key = filename.replace(".json", "")
                path = os.path.join(BASE_DIR, filename)

                with open(path, "r", encoding="utf-8") as f:
                    data = json.load(f)

                # Vue에서 보여줄 카테고리 정보
                self.categories[key] = {
                    "key": key,
                    "label": CATEGORY_LABELS.get(key, key), 
                    "examples": data  # 예시 질문
                }

                # 문서 데이터 적재
                for item in data:
                    self.documents.append({
                        "category": key,
                        "question": item["question"],
                        "answer": item["answer"]
                    })
                    self.doc_texts.append(item["question"])

        self._build_embeddings()

    def _build_embeddings(self):
        print("▶ 임베딩 생성 중…")

        self.embeddings = EMBED_MODEL.encode(
            self.doc_texts,
            convert_to_numpy=True,
            normalize_embeddings=True   # L2 normalize
        ).astype("float32")

        print(f"✔ Knowledge Loaded: {len(self.documents)} entries")

    def retrieve(self, query: str, top_k=3):
        query_vec = EMBED_MODEL.encode([query], convert_to_numpy=True, normalize_embeddings=True)

        # 코사인 유사도 계산
        sims = cosine_similarity(query_vec, self.embeddings)[0]

        # 상위 K개 인덱스
        idxs = np.argsort(sims)[::-1][:top_k]

        results = []
        for idx in idxs:
            doc = self.documents[idx]
            results.append({
                "similarity": float(sims[idx]),
                "question": doc["question"],
                "answer": doc["answer"],
                "category": doc["category"]
            })

        return results


kb = KnowledgeBase()
kb.load_all()
