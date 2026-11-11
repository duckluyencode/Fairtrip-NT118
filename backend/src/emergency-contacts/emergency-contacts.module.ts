import { Module } from '@nestjs/common';
import { EmergencyContactsController } from './emergency-contacts.controller.js';
import { EmergencyContactsService } from './emergency-contacts.service.js';

@Module({
  controllers: [EmergencyContactsController],
  providers: [EmergencyContactsService],
})
export class EmergencyContactsModule {}
