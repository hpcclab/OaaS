import logging
import os
import time
import uuid

import requests
from flask import Flask, request, make_response

logging.basicConfig(level=logging.DEBUG)
app = Flask(__name__)

if os.name == 'nt':
  SHELL = 'pwsh'
else:
  SHELL = 'sh'

KEY_NAME = "video"


def run_ffmpeg(args,
               tmp_in,
               tmp_out):
  resolution = args.get('RESOLUTION', '720x480')
  acodec = args.get('ACODEC', 'copy')
  vcodec = args.get('VCODEC', '')
  if resolution != 'no':
    resolution_cmd = f'-s {resolution}'
  else:
    resolution_cmd = ''
  codec = ''
  if acodec != '':
    codec += ' -acodec ' + acodec
  if vcodec != '':
    codec += ' -vcodec ' + vcodec
  cmd = f'ffmpeg -hide_banner -f mp4 -loglevel warning -y -i {tmp_in} {resolution_cmd} {codec} {tmp_out}'
  full_cmd = f'{SHELL} -c "{cmd}"'
  app.logger.warning(f'full_cmd = {full_cmd}')
  code = os.system(full_cmd)
  if code != 0:
    return f"Fail to execute {cmd}"


def load_file(session,
              src_url,
              tmp_in, id):
  start_ts = time.time()
  with session.get(src_url, allow_redirects=True, stream=True) as r:
    r.raise_for_status()
    with open(tmp_in, 'wb') as f:
      for chunk in r.iter_content(chunk_size=8192):
        f.write(chunk)
  app.logger.warning(f"load file of oid '{id}' in {time.time() - start_ts} s")


def save_file(session,
              alloc_url,
              tmp_out,
              output_id):
  resp = session.get(alloc_url)
  resp.raise_for_status()
  resp_json = resp.json()
  output_url = resp_json[KEY_NAME]

  start_ts = time.time()
  with open(tmp_out, 'rb') as file_data:
    resp = session.put(output_url, data=file_data)
    resp.raise_for_status()
  app.logger.warning(f"Save file of oid '{output_id}' in {time.time() - start_ts} s")


def make_completion(output_id: str,
                    task: dict,
                    error: str = None,
                    record: dict = None):
  success = error is None
  app.logger.warning(f'Execute task {output_id} success="{success}" error="{error}"')
  body = {
    "id": output_id,
    "success": success,
    "ext": {'osts': str(task['ts'])}
  }
  if error is not None:
    body['errorMsg'] = error
  if record is not None:
    body['embeddedRecord'] = record
  response = make_response(body)
  response.status_code = 200
  response.headers["Ce-Id"] = output_id
  response.headers["Ce-specversion"] = "1.0"
  response.headers["Ce-Source"] = "oaas/transcode"
  response.headers["Ce-Type"] = "oaas.task.result"
  return response


@app.route('/', methods=['POST'])
def handle():
  error_msg = None
  body = request.get_json(force=True)

  output_id = body['output']['id']
  main_id = body['main']['id']
  alloc_url = body['allocOutputUrl']
  output_obj = body['output']
  args = output_obj.get('origin', {}).get('args', {})
  video_format = args.get('FORMAT', 'mp4')

  src_url = body['mainKeys'][KEY_NAME]
  tmp_in = f"in-{uuid.uuid4()}.mp4"
  tmp_out = str(uuid.uuid4()) + '.' + video_format

  with requests.Session() as session:
    load_file(session, src_url, tmp_in, main_id)
    err = run_ffmpeg(args, tmp_in, tmp_out)
    if err is not None:
      error_msg = err

    save_file(session, alloc_url, tmp_out, output_id)

  if os.path.isfile(tmp_out):
    os.remove(tmp_out)
  if os.path.isfile(tmp_in):
    os.remove(tmp_in)

  return make_completion(output_id,
                         body,
                         error_msg)


if __name__ == '__main__':
  app.run(
    host='0.0.0.0',
    port=8080)