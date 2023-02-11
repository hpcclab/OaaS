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


class JsonHandler(oaas.Handler):
    def handle(self, ctx: OaasInvocationCtx):
        if ctx.task.main_obj.data is not None:
            record = ctx.task.main_obj.data.copy()
        else:
            record = {}
        entries = int(ctx.args.get('ENTRIES', '10'))
        keys = int(ctx.args.get('KEYS', '10'))
        values = int(ctx.args.get('VALUES', '10'))
        inplace = ctx.args.get('INPLACE', 'true').lower() == 'true'
        print(f"inplace {inplace}")

        for _ in range(entries):
            record[generate_text(keys)] = generate_text(values)

        record['ts'] = round(time.time() * 1000)
        if inplace:
            ctx.task.main_obj.data = record
        if ctx.task.output_obj is not None:
            ctx.task.output_obj.data = record
        return ctx.create_completion(success=True)


app = FastAPI()
router = oaas.Router()
router.register("example.json-update", JsonHandler())


@app.post('/')
async def handle(request: Request):
    body = await request.json()
    resp = router.handle_task(body)
    logging.info(f"completion {resp}")
    return resp

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
