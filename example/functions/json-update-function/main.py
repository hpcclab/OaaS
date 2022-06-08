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
    entries = int(args.get('ENTRIES', '10'))
    keys = int(args.get('KEYS', '10'))
    values = int(args.get('VALUES', '10'))

    for _ in range(entries):
      record[generate_text(keys)] = generate_text(values)

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
