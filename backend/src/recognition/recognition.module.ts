import { Module } from '@nestjs/common';
import { PlacesModule } from '../places/places.module.js';
import { RecognitionController } from './recognition.controller.js';
import { RecognitionService } from './recognition.service.js';
import { HttpVisionClient, VisionClient } from './vision.client.js';

@Module({
  imports: [PlacesModule],
  controllers: [RecognitionController],
  providers: [
    RecognitionService,
    { provide: VisionClient, useClass: HttpVisionClient },
  ],
})
export class RecognitionModule {}
