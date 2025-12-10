from openai import OpenAI
from app.chat.config import OPENAI_API_KEY, GPT_MODEL
from app.chat.retrieval import kb

client = OpenAI(api_key=OPENAI_API_KEY)
SIM_THRESHOLD = 0.9

async def process_chat(category: str, message: str):
    docs = [d for d in kb.documents if d["category"] == category]

    if not docs:
        return "해당 카테고리에 대한 지식이 없습니다."

    # 벡터 검색
    results = kb.retrieve(message, top_k=3)
    best = results[0]


    if best["similarity"] >= SIM_THRESHOLD:
        return best["answer"]

    # GPT fallback
    prompt = f"""
아래는 FAQ에서 찾은 가장 유사한 질문/답변입니다.
유사도가 낮으므로 참고만 하고, 사용자 질문에 맞춰 새 답변을 생성하세요.

[참고 질문] {best['question']}
[참고 답변] {best['answer']}

[사용자 질문]
{message}
"""

    completion = client.chat.completions.create(
        model=GPT_MODEL,
        messages=[
            {"role": "system", "content": "너는 ORDER101 챗봇이다."},
            {"role": "user", "content": prompt},
        ],
        max_tokens=300,
        temperature=0.2,
    )

    return completion.choices[0].message.content
