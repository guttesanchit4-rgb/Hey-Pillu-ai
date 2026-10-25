import os
import time
from collections import defaultdict, deque
from typing import Literal
import httpx
from fastapi import FastAPI, Header, HTTPException, Request
from pydantic import BaseModel, Field

app = FastAPI(title="Hey Pillu AI Backend", version="0.2.0")
BACKEND_TOKEN = os.getenv("PILLU_BACKEND_SECRET", "")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
MODEL = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
requests_by_ip = defaultdict(deque)

class Message(BaseModel):
    role: Literal["user", "assistant"]
    content: str = Field(min_length=1, max_length=2000)

class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=2000)
    mode: Literal["assistant", "girlfriend"] = "assistant"
    history: list[Message] = Field(default_factory=list, max_length=12)

class ChatResponse(BaseModel):
    reply: str
    mode: str

@app.middleware("http")
async def basic_rate_limit(request: Request, call_next):
    if request.url.path == "/chat":
        ip = request.client.host if request.client else "unknown"
        now = time.time()
        q = requests_by_ip[ip]
        while q and now - q[0] > 60:
            q.popleft()
        if len(q) >= 20:
            return __import__("starlette.responses").responses.JSONResponse(
                status_code=429, content={"detail": "Rate limit exceeded; try again shortly."}
            )
        q.append(now)
    return await call_next(request)

@app.get("/health")
async def health():
    return {"status": "healthy", "service": "Hey Pillu AI"}

@app.post("/chat", response_model=ChatResponse)
async def chat(body: ChatRequest, authorization: str | None = Header(default=None)):
    if not BACKEND_TOKEN:
        raise HTTPException(status_code=503, detail="PILLU_BACKEND_SECRET is not configured")
    if authorization != f"Bearer {BACKEND_TOKEN}":
        raise HTTPException(status_code=401, detail="Invalid token")
    if not OPENAI_API_KEY:
        return ChatResponse(reply="Backend चालू आहे, पण OPENAI_API_KEY सेट केलेली नाही.", mode=body.mode)

    system = (
        "तुम्ही Hey Pillu AI आहात. वापरकर्त्याला स्पष्ट, उपयुक्त मराठीत उत्तर द्या; गरजेनुसार हिंदी किंवा इंग्रजी वापरा."
        if body.mode == "assistant" else
        "तुम्ही प्रेमळ, खट्याळ पण आदर राखणारी मराठी संवादसाथी आहात. स्वतःला खरा मनुष्य असल्याचा दावा करू नका."
    )
    messages = [{"role": "system", "content": system}]
    messages.extend({"role": m.role, "content": m.content} for m in body.history[-8:])
    messages.append({"role": "user", "content": body.message})
    try:
        async with httpx.AsyncClient(timeout=httpx.Timeout(20.0)) as client:
            resp = await client.post(
                "https://api.openai.com/v1/chat/completions",
                headers={"Authorization": f"Bearer {OPENAI_API_KEY}"},
                json={"model": MODEL, "messages": messages, "temperature": 0.6, "max_tokens": 600},
            )
        if resp.status_code >= 400:
            raise HTTPException(status_code=502, detail=f"AI provider returned HTTP {resp.status_code}")
        data = resp.json()
        answer = data["choices"][0]["message"]["content"]
        return ChatResponse(reply=answer, mode=body.mode)
    except HTTPException:
        raise
    except httpx.TimeoutException:
        raise HTTPException(status_code=504, detail="AI request timed out")
    except Exception:
        raise HTTPException(status_code=502, detail="AI service unavailable")
