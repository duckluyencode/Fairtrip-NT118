import {
  Body,
  Controller,
  Get,
  HttpStatus,
  INestApplication,
  Post,
} from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { Type } from 'class-transformer';
import { IsEmail, IsInt, Min, ValidateNested } from 'class-validator';
import request from 'supertest';
import { App } from 'supertest/types.js';
import { configureApp } from '../../app.setup.js';
import { AppError } from './app-error.js';

class AddressDto {
  @IsInt()
  @Min(1)
  floor: number;
}

class SampleDto {
  @IsEmail()
  email: string;

  @ValidateNested()
  @Type(() => AddressDto)
  address: AddressDto;
}

@Controller('sample')
class SampleController {
  @Post()
  create(@Body() body: SampleDto) {
    return body;
  }

  @Get('app-error')
  appError() {
    throw new AppError(
      HttpStatus.CONFLICT,
      'ACCOUNT_EXISTS',
      'Account already exists',
    );
  }

  @Get('crash')
  crash() {
    throw new Error('database password is wrong');
  }
}

describe('error responses', () => {
  let app: INestApplication<App>;

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({
      controllers: [SampleController],
    }).compile();
    // No logger: the 500 case would print its stack trace on every run.
    app = moduleRef.createNestApplication({ logger: false });
    configureApp(app);
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  it('lists every invalid field, including nested ones', async () => {
    const res = await request(app.getHttpServer())
      .post('/api/v1/sample')
      .send({ email: 'not-an-email', address: { floor: 0 } })
      .expect(400);

    expect(res.body).toMatchObject({
      statusCode: 400,
      code: 'VALIDATION_FAILED',
    });
    expect(res.body.details).toEqual([
      { field: 'email', messages: ['email must be an email'] },
      { field: 'address.floor', messages: ['floor must not be less than 1'] },
    ]);
  });

  it('rejects fields the DTO does not declare', async () => {
    const res = await request(app.getHttpServer())
      .post('/api/v1/sample')
      .send({ email: 'a@b.co', address: { floor: 2 }, isAdmin: true })
      .expect(400);

    expect(res.body.details).toEqual([
      { field: 'isAdmin', messages: ['property isAdmin should not exist'] },
    ]);
  });

  it('converts nested input into DTO instances', async () => {
    await request(app.getHttpServer())
      .post('/api/v1/sample')
      .send({ email: 'a@b.co', address: { floor: 2 } })
      .expect(201)
      .expect({ email: 'a@b.co', address: { floor: 2 } });
  });

  it('keeps the code of an AppError', () => {
    return request(app.getHttpServer())
      .get('/api/v1/sample/app-error')
      .expect(409)
      .expect({
        statusCode: 409,
        code: 'ACCOUNT_EXISTS',
        message: 'Account already exists',
      });
  });

  it('hides the details of unexpected errors', () => {
    return request(app.getHttpServer())
      .get('/api/v1/sample/crash')
      .expect(500)
      .expect({
        statusCode: 500,
        code: 'INTERNAL_ERROR',
        message: 'Internal server error',
      });
  });
});
