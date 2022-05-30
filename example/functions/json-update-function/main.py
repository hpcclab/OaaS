import random
import string
import time

from fastapi import Request, Response, FastAPI

# logging.basicConfig(level=logging.INFO)
app = FastAPI()


def generate_text(num):
  letters = string.ascii_lowercase
  return ''.join(random.choice(letters) for _ in range(num))


@app.post('/')
async def handle(request: Request,
                 response: Response):
    body = await request.json()
    output_obj = body['output']
    args = output_obj['origin'].get('args', {})
    record = body['main'].get('embeddedRecord', {})
    no_keys = args.get('NO_KEYS', 10)
    key_len = args.get('KEY_LEN', 10)
    val_len = args.get('VAL_LEN', 10)

    for _ in range(no_keys):
      record[generate_text(key_len)] = generate_text(val_len)

    response.headers["Ce-Id"] = str(output_obj['id'])
    response.headers["Ce-specversion"] = "1.0"
    response.headers["Ce-Source"] = "oaas/json-update"
    response.headers["Ce-Type"] = "oaas.task.result"
    record['ts'] = round(time.time() * 1000)
    return {
      'id': output_obj['id'],
      'success': True,
      'embeddedRecord': record
    }
