import logging
import random
import string
import time

import aiohttp
import oaas_sdk_py as oaas
import uvicorn
from fastapi import Request, FastAPI
from oaas_sdk_py import OaasInvocationCtx

logging.basicConfig(level=logging.DEBUG)


class ConcatHandler(oaas.Handler):
    async def handle(self, ctx: OaasInvocationCtx):
        append = ctx.args.get('APPEND', '')
        inplace = ctx.args.get('INPLACE', 'false').lower() == 'true'

        record = ctx.task.main_obj.data.copy() if ctx.task.main_obj.data is not None else {}

        record['ts'] = round(time.time() * 1000)
        resp = await ctx.load_main_file("text")
        old_text = await resp.read()
        text = old_text.decode("utf-8") + append
        b_text = bytes(text, 'utf-8')

        async with aiohttp.ClientSession() as session:
            if inplace:
                await ctx.upload_main_byte_data(session, "text", b_text)
            else:
                await ctx.upload_byte_data(session, "text", b_text)


app = FastAPI()
router = oaas.Router()
router.register("example.concat", ConcatHandler())


@app.post('/')
async def handle(request: Request):
    body = await request.json()
    logging.debug(f"request {body}")
    resp = await router.handle_task(body)
    logging.debug(f"completion {resp}")
    return resp

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
