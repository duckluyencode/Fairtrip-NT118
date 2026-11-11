import { Module } from '@nestjs/common';
import { AccountDeletionService } from './account-deletion.service.js';
import { UsersController } from './users.controller.js';

@Module({
  controllers: [UsersController],
  providers: [AccountDeletionService],
})
export class UsersModule {}
