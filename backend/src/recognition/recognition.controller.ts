import {
  Controller,
  HttpCode,
  HttpStatus,
  Post,
  UploadedFile,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { Throttle } from '@nestjs/throttler';
import { ApiBearerAuth, ApiBody, ApiConsumes, ApiTags } from '@nestjs/swagger';
import { CurrentUser, type AuthUser } from '../auth/auth.decorators.js';
import { RecognitionResponse } from './recognition.response.js';
import {
  RecognitionService,
  type UploadedImage,
} from './recognition.service.js';

/** Photos are resized by the app before upload; Cloud Vision needs no more. */
export const MAX_IMAGE_BYTES = 4 * 1024 * 1024;

@ApiTags('places')
@ApiBearerAuth()
@Controller('places')
export class RecognitionController {
  constructor(private readonly recognition: RecognitionService) {}

  /**
   * Recognizes landmarks in a photo (multipart field "image", JPEG/PNG/WebP, at most
   * 4 MB) and returns the matching places, without prices. Each call is billed by
   * Google, so it is limited to 10 per minute.
   */
  @Post('recognize')
  @HttpCode(HttpStatus.OK)
  @Throttle({ default: { limit: 10, ttl: 60_000 } })
  @UseInterceptors(
    FileInterceptor('image', {
      limits: { fileSize: MAX_IMAGE_BYTES, files: 1 },
    }),
  )
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      required: ['image'],
      properties: { image: { type: 'string', format: 'binary' } },
    },
  })
  recognize(
    @CurrentUser() me: AuthUser,
    @UploadedFile() image: UploadedImage | undefined,
  ): Promise<RecognitionResponse> {
    return this.recognition.recognize(me.id, image);
  }
}
