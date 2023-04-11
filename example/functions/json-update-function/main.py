import logging
import random
import string
import time

import oaas_sdk_py as oaas
import uvicorn
from fastapi import Request, FastAPI
from oaas_sdk_py import OaasInvocationCtx

logging.basicConfig(level=logging.INFO)


def generate_text(num):
  letters = string.ascii_lowercase
  return ''.join(random.choice(letters) for _ in range(num))


class RandomHandler(oaas.Handler):
    async def handle(self, ctx: OaasInvocationCtx):

        entries = int(ctx.args.get('ENTRIES', '10'))
        keys = int(ctx.args.get('KEYS', '10'))
        values = int(ctx.args.get('VALUES', '10'))
        inplace = ctx.args.get('INPLACE', 'true').lower() == 'true'

        record = ctx.task.main_obj.data.copy() if ctx.task.main_obj.data is not None else {}

        for _ in range(entries):
            record[generate_text(keys)] = generate_text(values)

        record['ts'] = round(time.time() * 1000)
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
router.register("example.record.random", RandomHandler())
router.register("example.record.merge", MergeHandler())


@app.post('/')
async def handle(request: Request):
    body = await request.json()
    logging.debug(f"request {body}")
    resp = await router.handle_task(body)
    logging.debug(f"completion {resp}")
    return resp

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
