import asyncio
import logging
import os
import time
import uuid

import aiofiles.os
import aiohttp
import oaas_sdk_py as oaas
import uvicorn
from PIL import Image
from fastapi import Request, FastAPI, HTTPException
from oaas_sdk_py import OaasInvocationCtx

LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
level = logging.getLevelName(LOG_LEVEL)
logging.basicConfig(level=level)


async def write_to_file(resp, file_path):
    with open(file_path, "wb") as f:
        async for chunk in resp.content.iter_chunked(1024):
            f.write(chunk)


def get_image_size(image_path):
    """
    Get the size of the image.

    Parameters:
    image_path (str): File path of the image.

    Returns:
    tuple: Size of the image in the format (width, height).
    """
    with Image.open(image_path) as img:
        width, height = img.size
        return width, height


async def resize_image(input_image, output_image, method='ffmpeg', size=None, ratio=None):
    """
    Resize the input image based on either size or ratio using either PIL or ffmpeg.

    Parameters:
    input_image (str): File path of the input image.
    output_image (str): File path of the output resized image.
    method (str): Method to use for resizing. Either 'ffmpeg' or 'PIL'.
    size (str): Desired size in the format "width,height" (e.g., "300x200").
                If None, ratio will be used for resizing.
    ratio (float): Desired resizing ratio (e.g., 0.5 for 50% smaller).
                   If None, size will be used for resizing.
    """
    if method == 'ffmpeg':
        if size:
            scale_option = f"scale={size}"
        elif ratio:
            scale_option = f"scale=iw*{ratio}:ih*{ratio}"
        else:
            raise ValueError("Either size or ratio must be provided.")

        # Execute ffmpeg command
        proc = await asyncio.create_subprocess_exec(
            'ffmpeg',
            "-hide_banner",
            "-loglevel", "warning",
            '-i', input_image,
            '-vf', scale_option,
            output_image
        )
        await proc.wait()
        if size:
            width, height = map(int, size.split('x'))
            return width, height
        return get_image_size(output_image)
    elif method == 'PIL':
        img = Image.open(input_image)

        if size:
            width, height = map(int, size.split('x'))
            img = img.resize((width, height))
        elif ratio:
            width, height = img.size
            new_width = int(width * ratio)
            new_height = int(height * ratio)
            img = img.resize((new_width, new_height))

        img.save(output_image)
        return img.size
    elif method == "none":
        await aiofiles.os.rename(input_image, output_image)
        return 0, 0
    else:
        raise ValueError("Invalid method. Choose either 'ffmpeg' or 'PIL'.")


class ResizeHandler(oaas.Handler):

    async def handle(self, ctx: OaasInvocationCtx):
        size = ctx.args.get('size', '')
        ratio = float(ctx.args.get('ratio', '1'))
        method = ctx.args.get("method", "ffmpeg")
        inplace = ctx.task.output_obj is None or ctx.task.output_obj.id is None
        req_ts = int(ctx.args.get('reqts', '0'))
        fmt = ctx.task.main_obj.data.get('format', 'png')
        tmp_in = f"in-{uuid.uuid4()}.{fmt}"
        tmp_out = f"out-{uuid.uuid4()}.{fmt}"

        record = ctx.task.main_obj.data.copy() if ctx.task.main_obj.data is not None else {}

        if req_ts != 0:
            record['reqts'] = req_ts

        start_ts = time.time()
        async with aiohttp.ClientSession() as session:
            async with await ctx.load_main_file(session, "image") as resp:
                await write_to_file(resp, tmp_in)
                loading_time = time.time() - start_ts
                logging.debug(f"load data in {loading_time} s")

                (width, height) = await resize_image(tmp_in, tmp_out,
                                                     method=method,
                                                     size=size,
                                                     ratio=ratio)
                start_ts = time.time()
                await ctx.upload_file(session, "image", tmp_out)
                uploading_time = time.time() - start_ts
                logging.debug(f"upload data in {uploading_time} s")
                record['ts'] = round(time.time() * 1000)
                record['load'] = round(loading_time * 1000)
                record['upload'] = round(uploading_time * 1000)
                record['width'] = width
                record['height'] = height
                if inplace:
                    ctx.task.main_obj.data = record
                else:
                    ctx.task.output_obj.data = record


app = FastAPI()
router = oaas.Router()
router.register(ResizeHandler())


@app.post('/')
async def handle(request: Request):
    body = await request.json()
    logging.debug("request %s", body)
    resp = await router.handle_task(body)
    logging.debug("completion %s", resp)
    if resp is None:
        logging.warning("No handler matched '%s'", body['funcKey'])
        raise HTTPException(status_code=404, detail="No handler matched")
    return resp


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
