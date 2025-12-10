from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from app.chat.engine import process_chat
from app.chat.retrieval import kb

router = APIRouter(prefix="/api/v1/chat")

class ChatReq(BaseModel):
    category: str
    message: str

@router.get("/meta")
async def get_meta():
    return {"categories": list(kb.categories.values())}

@router.post("/stream")
async def stream_chat(req: ChatReq):
    async def event():
        result = await process_chat(req.category, req.message)
        yield result.encode("utf-8")

    return StreamingResponse(event(), media_type="text/plain")
