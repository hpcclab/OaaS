import logging
import os
import uuid

import requests
from flask import Flask, request, make_response

logging.basicConfig(level=logging.DEBUG)
app = Flask(__name__)

if os.name == 'nt':
  shell = 'pwsh'
else:
  shell = 'sh'


def list_hls(prefix):
  return [file for file in os.listdir(".") if file.startswith(prefix)]


def allocate(prefix,
             alloc_url):
  keys = list_hls(prefix)
  r = requests.post(alloc_url, json=keys)
  if r.status_code != 200:
    raise "Got error when allocate keys"
  return r.json()


def run_and_log(cmd):
  full_cmd = f'{shell} -c "{cmd}"'
  app.logger.warning(f'full_cmd = {full_cmd}')
  return os.system(full_cmd)


@app.route('/', methods=['POST'])
def handle():
  status_code = 200
  resp_msg = "Successfully execute task"
  body = request.get_json(force=True)

  new_uuid = body['output']['id']
  alloc_url = body['allocOutputUrl']
  output_obj = body['output']
  args = output_obj.get('origin', {}).get('args', {})
  file_name = args.get('KEY', 'video')

  src_url = body['mainKeys'][file_name]
  resolution = args.get('RESOLUTION', 'no')
  if resolution != 'no':
    resolution_cmd = f'-s {resolution}'
  else:
    resolution_cmd = ''
  acodec = args.get('ACODEC', 'copy')
  vcodec = args.get('VCODEC', 'copy')
  preset = args.get('PRESET', 'ultrafast')
  codec = '-ac:a 2 '
  if acodec != '':
    codec += ' -c:a ' + acodec
  if vcodec != '':
    codec += ' -c:v ' + vcodec
  if preset != '':
    preset = f' -preset {preset}'

  tmp_in = f"in-{uuid.uuid4()}.mp4"
  run_and_log(f"curl -sS -L -o {tmp_in} {src_url}")
  prefix = 'video'
  tmp_file = prefix + '.m3u8'
  cmd = f'ffmpeg -hide_banner -loglevel warning -y -i {tmp_in} {preset} -g 30' \
        f' {resolution_cmd} {codec} -hls_time 1 -hls_list_size 0  -f hls {tmp_file}'
  code = run_and_log(cmd)
  # if code != 0:
  #   resp_msg = f"Fail to execute {cmd}"
  #   status_code = 500

  upload_urls = allocate(prefix, alloc_url)

  for file, output_url in upload_urls.items():
    cmd = f'curl -sS -T {file} \'{output_url}\''
    code = run_and_log(cmd)
    if code != 0:
      resp_msg = f"Fail to execute {cmd}"
      status_code = 500

  cmd = f'rm -f {tmp_in} {prefix}*'
  code = run_and_log(cmd)
  if code != 0:
    resp_msg = f"Fail to execute {cmd}"
    status_code = 500

  app.logger.warning(f'Successfully execute task {new_uuid}')
  return make_resonse(resp_msg, status_code, new_uuid)


def make_resonse(msg,
                 status_code,
                 new_uuid):
  response = make_response({
    "msg": msg
  })
  response.status_code = status_code
  response.headers["Ce-Id"] = str(new_uuid)
  response.headers["Ce-specversion"] = "1.0"
  response.headers["Ce-Source"] = "oaas/tohls"
  response.headers["Ce-Type"] = "oaas.task.result"
  response.headers["Ce-Tasksucceeded"] = "true"
  return response


if __name__ == '__main__':
  app.run(
    host='0.0.0.0',
    port=8080)
