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


@app.route('/', methods=['POST'])
def handle():
  error_msg = None
  body = request.get_json(force=True)

  output_id = body['output']['id']
  alloc_url = body['allocOutputUrl']
  output_obj = body['output']
  args = output_obj.get('origin', {}).get('args', {})

  src_url = body['mainKeys'][KEY_NAME]
  resolution = args.get('RESOLUTION', '720x480')
  acodec = args.get('ACODEC', 'copy')
  vcodec = args.get('VCODEC', '')
  video_format = args.get('FORMAT', 'mp4')

  if resolution != 'no':
    resolution_cmd = f'-s {resolution}'
  else:
    resolution_cmd = ''
  codec = ''
  if acodec != '':
    codec += ' -acodec ' + acodec
  if vcodec != '':
    codec += ' -vcodec ' + vcodec

  tmp_in = f"in-{uuid.uuid4()}.mp4"
  # os.system(f"curl -L -o {tmp_in} {src_url}")
  startTs = time.time()
  with requests.get(src_url, stream=True) as r:
    r.raise_for_status()
    with open(tmp_in, 'wb') as f:
      for chunk in r.iter_content(chunk_size=8192):
        f.write(chunk)
  print(f"load file from '{src_url}' in {time.time() - startTs} s")

  tmp_file = str(uuid.uuid4()) + '.' + video_format
  cmd = f'ffmpeg -hide_banner -f mp4 -loglevel warning -y -i {tmp_in} {resolution_cmd} {codec} {tmp_file}'
  full_cmd = f'{SHELL} -c "{cmd}"'
  app.logger.warning(f'full_cmd = {full_cmd}')
  code = os.system(full_cmd)
  if code != 0:
    error_msg = f"Fail to execute {cmd}"

  r = requests.get(alloc_url)
  if r.status_code != 200:
    error_msg = "Got error when allocate keys"
  resp_json = r.json()
  output_url = resp_json[KEY_NAME]

  # cmd = f'curl -T {tmp_file} \'{output_url}\''
  # full_cmd = f'{SHELL} -c "{cmd}"'
  # app.logger.warning(f'full_cmd = {full_cmd}')
  #
  # code = os.system(f'{SHELL} -c "{cmd}"')
  # if code != 0:
  #   error_msg = f"Fail to execute {cmd}"
  with open(tmp_file, 'rb') as file_data:
    rspn = requests.put(output_url, data=file_data)
    if rspn.status_code >= 400:
      error_msg = "Fail to persist output file"
  print(f"Save file to '{output_url}' in {time.time() - startTs} s")

  if os.path.isfile(tmp_file):
    os.remove(tmp_file)
  if os.path.isfile(tmp_in):
    os.remove(tmp_in)

  return make_completion(output_id,
                         error_msg)


def make_completion(output_id: str,
                    error: str = None,
                    record: dict = None):
  success = error is None
  app.logger.warning(f'Execute task {output_id} success="{success}" error="{error}"')
  body = {
    "id": output_id,
    "success": success
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


if __name__ == '__main__':
  app.run(
    host='0.0.0.0',
    port=8080)
