import uuid

from flask import Flask, request, make_response
import requests
import os
import logging

logging.basicConfig(level=logging.DEBUG)
app = Flask(__name__)

if os.name == 'nt':
    shell = 'pwsh'
else:
    shell = 'sh'


@app.route('/', methods=['POST'])
def handle():
    status_code = 200
    resp_msg = "Successfully execute task"
    body = request.get_json(force=True)

    new_uuid = body['output']['id']
    alloc_url = body['allocOutputUrl']
    output_obj = body['output']
    args = output_obj['origin']['args']
    file_name = args.get('KEY', 'video')

    src_url = body['mainKeys'][file_name]
    resolution = args.get('RESOLUTION', '720x480')
    if resolution != 'no':
        resolution_cmd = f'-s {resolution}'
    else:
        resolution_cmd = ''
    acodec = args.get('ACODEC', 'copy')
    vcodec = args.get('VCODEC', '')
    codec = ''
    if acodec != '':
        codec += ' -acodec ' + acodec
    if vcodec != '':
        codec += ' -vcodec ' + vcodec

    video_format = args.get('FORMAT', 'mp4')

    tmp_in = f"in-{uuid.uuid4()}.mp4";
    os.system(f"curl -L -o {tmp_in} {src_url}")

    tmp_file = str(uuid.uuid4()) + '.' + video_format
    # cmd = f'ffmpeg -hide_banner -f mp4 -loglevel warning -y -i {src_url} {resolution_cmd} {codec} {tmp_file}'
    cmd = f'ffmpeg -hide_banner -f mp4 -loglevel warning -y -i {tmp_in} {resolution_cmd} {codec} {tmp_file}'
    full_cmd = f'{shell} -c "{cmd}"'
    app.logger.warning(f'full_cmd = {full_cmd}')
    code = os.system(full_cmd)
    if code != 0:
        resp_msg = f"Fail to execute {cmd}"
        status_code = 500

    r = requests.get(alloc_url)
    if r.status_code != 200:
        raise "Got error when allocate keys"
    resp_json = r.json()
    output_url = resp_json[file_name]

    cmd = f'curl -T {tmp_file} \'{output_url}\''
    full_cmd = f'{shell} -c "{cmd}"'
    app.logger.warning(f'full_cmd = {full_cmd}')

    code = os.system(f'{shell} -c "{cmd}"')
    if code != 0:
        resp_msg = f"Fail to execute {cmd}"
        status_code = 500
    code = os.system(f'{shell} -c "rm {tmp_file}"')
    if code != 0:
        resp_msg = f"Fail to execute rm {tmp_file}"
        status_code = 500

    app.logger.warning(f'Successfully execute task {output_url}')
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
    response.headers["Ce-Source"] = "oaas/transcode"
    response.headers["Ce-Type"] = "oaas.task.result"
    response.headers["Ce-Tasksucceeded"] = "true"
    return response


if __name__ == '__main__':
    app.run(
        host='0.0.0.0',
        port=8080)
