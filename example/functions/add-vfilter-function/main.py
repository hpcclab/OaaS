import logging
import os

import requests
from flask import Flask, request, make_response
from furl import furl

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

  src_url = body['mainKeys']['*']
  parsed_url = furl(src_url)
  parsed_url /= "video.m3u8"
  src_url = parsed_url.url

  vf = args.get('VF', 'boxblur=10')
  preset = args.get('PRESET', 'ultrafast')
  if preset != '':
    preset = f' -preset {preset}'

  prefix = 'video'
  tmp_file = prefix + '.m3u8'
  cmd = f'ffmpeg -hide_banner -loglevel warning -y -i {src_url} {preset} -g 30' \
        f' -vf \'{vf}\' -hls_time 1 -hls_list_size 0  -f hls {tmp_file}'
  code = run_and_log(cmd)
  if code != 0:
    resp_msg = f"Fail to execute {cmd}"
    status_code = 500

  upload_urls = allocate(prefix, alloc_url)

  for file, output_url in upload_urls.items():
    cmd = f'curl -sS -T {file} \'{output_url}\''
    code = run_and_log(cmd)
    if code != 0:
      resp_msg = f"Fail to execute {cmd}"
      status_code = 500

  cmd = f'rm -f {prefix}*'
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
  response.headers["Ce-Source"] = "oaas/add-vfilter"
  response.headers["Ce-Type"] = "oaas.task.result"
  response.headers["Ce-Tasksucceeded"] = "true"
  return response


if __name__ == '__main__':
  app.run(
    host='0.0.0.0',
    port=8080)
