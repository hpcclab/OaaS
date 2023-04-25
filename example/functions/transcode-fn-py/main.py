import logging
import os
import uuid

import aiofiles
import aiohttp
import oaas_sdk_py
import uvicorn
from fastapi import FastAPI, Request, HTTPException
from oaas_sdk_py import OaasInvocationCtx

logging.basicConfig(level=logging.DEBUG)

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
  logging.warning(f'full_cmd = {full_cmd}')
  code = os.system(full_cmd)
  if code != 0:
    return f"Fail to execute {cmd}"


class TranscodeHandler(oaas_sdk_py.Handler):
  async def handle(self, ctx: OaasInvocationCtx):
    video_format = ctx.args.get('FORMAT', 'mp4')
    tmp_in = f"in-{uuid.uuid4()}.mp4"
    tmp_out = str(uuid.uuid4()) + '.' + video_format

    async with aiohttp.ClientSession() as session:
      resp = await ctx.load_main_file(session, "video")
      resp.raise_for_status()
      async with aiofiles.open(tmp_in, "wb") as f:
        async for chunk in resp.content.iter_chunked(1024):
          await f.write(chunk)
      run_ffmpeg(ctx.args, tmp_in, tmp_out)
      await ctx.upload_file(session, "video", tmp_out)
    if os.path.isfile(tmp_out):
      os.remove(tmp_out)
    if os.path.isfile(tmp_in):
      os.remove(tmp_in)


app = FastAPI()
router = oaas_sdk_py.Router()
router.register("example.video.transcode", TranscodeHandler())


@app.post('/')
async def handle(request: Request):
  body = await request.json()
  logging.debug(f"request {body}")
  resp = await router.handle_task(body)
  logging.debug(f"completion {resp}")
  if resp is None:
    raise HTTPException(status_code=404, detail="No handler matched")
  return resp


if __name__ == "__main__":
  uvicorn.run(app, host="0.0.0.0", port=8080)
