import logging
import os
import time

import aiohttp
import oaas_sdk_py as oaas
import uvicorn
from fastapi import Request, FastAPI, HTTPException
from oaas_sdk_py import OaasInvocationCtx

LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
level = logging.getLevelName(LOG_LEVEL)
logging.basicConfig(level=level)


class ConcatHandler(oaas.Handler):
    async def handle(self, ctx: OaasInvocationCtx):
        append = ctx.args.get('APPEND', '')
        inplace = ctx.task.output_obj is None or ctx.task.output_obj.id is None
        req_ts = int(ctx.args.get('reqts', '0'))

        record = ctx.task.main_obj.data.copy() if ctx.task.main_obj.data is not None else {}

        if req_ts != 0:
            record['reqts'] = req_ts

        start_ts = time.time()
        async with aiohttp.ClientSession() as session:
            async with await ctx.load_main_file(session, "text") as resp:
                old_text = await resp.read()
                loading_time = time.time() - start_ts
                logging.debug(f"load data in {loading_time} s")

                text = old_text.decode("utf-8") + append
                b_text = bytes(text, 'utf-8')
                start_ts = time.time()
                if inplace:
                    await ctx.upload_main_byte_data(session, "text", b_text)
                else:
                    await ctx.allocate_file(session)
                    logging.debug(f"allocate url in {time.time() - start_ts} s")
                    start_ts = time.time()
                    await ctx.upload_byte_data(session, "text", b_text)
                uploading_time = time.time() - start_ts
                logging.debug(f"upload data in {uploading_time} s")
                record['ts'] = round(time.time() * 1000)
                record['load'] = round(loading_time * 1000)
                record['upload'] = round(uploading_time * 1000)
                if inplace:
                    ctx.task.main_obj.data = record
                else:
                    ctx.task.output_obj.data = record


app = FastAPI()
router = oaas.Router()
router.register(ConcatHandler())
router.register("example.text.concat", ConcatHandler())


@app.post('/')
async def handle(request: Request):
    body = await request.json()
    logging.debug("request %s", body)
    resp = await router.handle_task(body)
    logging.debug("completion %s", resp)
    if resp is None:
        logging.warning("No handler matched '%s'", body['funcKey'])
        raise HTTPException(status_code=404, detail="No handler matched")
    return resp


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
