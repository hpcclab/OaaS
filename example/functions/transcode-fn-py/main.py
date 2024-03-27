import asyncio
import logging
import os
import subprocess
import time
import uuid

import aiofiles.os
import aiohttp
import oaas_sdk_py
import uvicorn
from fastapi import FastAPI, Request, HTTPException
from oaas_sdk_py import OaasInvocationCtx
from starlette.concurrency import run_in_threadpool

LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
VIDEO_KEY = os.getenv("VIDEO_KEY", "video")
USE_THREAD_POOL = os.getenv("THREAD_POOL", "true") == "true"
level = logging.getLevelName(LOG_LEVEL)
logging.basicConfig(level=level)

if os.name == 'nt':
    SHELL = 'pwsh'
else:
    SHELL = 'sh'


def _run_ffmpeg(resolution,
                acodec,
                vcodec,
                tmp_in,
                tmp_out):
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
    full_cmd = f'sh -c "{cmd}"'
    ts = time.time()
    logging.info(f'full_cmd = {full_cmd}')
    proc = subprocess.Popen(cmd.split())
    proc.wait()
    logging.info(f"done ffmpeg in {time.time() - ts} s")
    if proc.returncode != 0:
        raise Exception(f"Fail to execute {cmd}")


async def run_ffmpeg_in_thread_pool(args,
                                    tmp_in,
                                    tmp_out):
    resolution = args.get('RESOLUTION', '1280x720')
    acodec = args.get('ACODEC', 'copy')
    vcodec = args.get('VCODEC', '')
    await run_in_threadpool(_run_ffmpeg,
                            resolution,
                            acodec,
                            vcodec,
                            tmp_in,
                            tmp_out)


async def run_ffmpeg(args,
                     tmp_in,
                     tmp_out):
    resolution = args.get('RESOLUTION', '1280x720')
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
    ts = time.time()
    logging.debug(f'full_cmd = {full_cmd}')
    process = await asyncio.create_subprocess_exec(SHELL, "-c", cmd)
    await process.wait()
    logging.debug(f"done ffmpeg in {time.time() - ts} s")
    if process.returncode != 0:
        return f"Fail to execute {cmd}"


class TranscodeHandler(oaas_sdk_py.Handler):
    async def handle(self, ctx: OaasInvocationCtx):
        video_format = ctx.args.get('FORMAT', 'mp4')
        tmp_in = f"in-{uuid.uuid4()}.mp4"
        tmp_out = str(uuid.uuid4()) + '.' + video_format
        req_ts = int(ctx.args.get('reqts', '0'))

        record = ctx.task.main_obj.data.copy() if ctx.task.main_obj.data is not None else {}
        if req_ts != 0:
            record['reqts'] = req_ts

        try:
            async with aiohttp.ClientSession() as session:
                ts = time.time()
                resp = await ctx.load_main_file(session, VIDEO_KEY)
                with open(tmp_in, "wb") as f:
                    async for chunk in resp.content.iter_chunked(1024):
                        f.write(chunk)
                logging.debug(f"done loading in {time.time() - ts} s")
                if USE_THREAD_POOL:
                    await run_ffmpeg_in_thread_pool(ctx.args, tmp_in, tmp_out)
                else:
                    await run_ffmpeg(ctx.args, tmp_in, tmp_out)
                ts = time.time()
                await ctx.upload_file(session, VIDEO_KEY, tmp_out)
                logging.debug(f"done uploading in {time.time() - ts} s")
        except Exception as e:
            ctx.success = False
            ctx.error = str(e)
        finally:
            if aiofiles.os.path.isfile(tmp_out):
                await aiofiles.os.remove(tmp_out)
            if aiofiles.os.path.isfile(tmp_in):
                await aiofiles.os.remove(tmp_in)
        record['ts'] = round(time.time() * 1000)
        ctx.task.output_obj.data = record


app = FastAPI()
router = oaas_sdk_py.Router()
router.register(TranscodeHandler())


@app.post('/')
async def handle(request: Request):
    body = await request.json()
    logging.debug("request %s", body)
    resp = await router.handle_task(body)
    logging.debug("completion %s", resp)
    if resp is None:
        raise HTTPException(status_code=404, detail="No handler matched")
    return resp


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
