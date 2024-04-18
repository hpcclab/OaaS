import logging
import random
import string
import time

import oaas_sdk_py as oaas
import uvicorn
from fastapi import Request, FastAPI, HTTPException
from oaas_sdk_py import OaasInvocationCtx
import os

LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
level = logging.getLevelName(LOG_LEVEL)
logging.basicConfig(level=level)


def generate_text(num):
  letters = string.ascii_lowercase
  return ''.join(random.choice(letters) for _ in range(num))


class RandomHandler(oaas.Handler):
    async def handle(self, ctx: OaasInvocationCtx):

        entries = int(ctx.args.get('ENTRIES', '10'))
        keys = int(ctx.args.get('KEYS', '10'))
        values = int(ctx.args.get('VALUES', '10'))
        max_keys = int(ctx.args.get('MAX', '10000'))
        inplace = ctx.args.get('INPLACE', 'true').lower() == 'true'
        req_ts = int(ctx.args.get('reqts', '0'))

        record = ctx.task.main_obj.data.copy() if ctx.task.main_obj.data is not None else {}

        for _ in range(entries):
            record[generate_text(keys)] = generate_text(values)
        count = len(record)
        if count > max_keys:
            for _ in range(count - max_keys):
                k = next(iter(record.keys()))
                record.pop(k)

        record['ts'] = round(time.time() * 1000)
        if req_ts > 0:
            record['reqts'] = req_ts
        if inplace:
            ctx.task.main_obj.data = record
        if ctx.task.output_obj is not None:
            ctx.task.output_obj.data = record


class MergeHandler(oaas.Handler):
    async def handle(self, ctx: OaasInvocationCtx):
        inplace = ctx.args.get('INPLACE', 'false').lower() == 'true'
        record = ctx.task.main_obj.data.copy() if ctx.task.main_obj.data is not None else {}

        for input_obj in ctx.task.inputs:
            other_record = input_obj.data.copy() if input_obj.data is not None else {}
            record = record | other_record

        if inplace:
            ctx.task.main_obj.data = record
        if ctx.task.output_obj is not None:
            ctx.task.output_obj.data = record


app = FastAPI()
router = oaas.Router()
router.register(RandomHandler())
router.register(RandomHandler(), "example.record.random")
router.register(MergeHandler(), "example.record.merge")


@app.post('/')
async def handle(request: Request):
    body = await request.json()
    logging.debug("request %s", body)
    resp = await router.handle_task(body)
    logging.debug("completion %s", resp)
    if resp is None:
        raise HTTPException(status_code=404, detail="No handler matched")
    return resp

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
