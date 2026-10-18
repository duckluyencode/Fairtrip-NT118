# Backend (NestJS)

API server cho app Android TripFair: xác thực, nguồn giá, thuật toán lộ trình và truy cập MySQL.
NestJS 12 + TypeScript (ESM), chạy trên Node.js 24 LTS. Quy tắc chung của dự án nằm ở [CLAUDE.md gốc](../CLAUDE.md).

## Lệnh thường dùng (chạy trong `backend/`)
- `npm install`: cài dependency
- `npm run start:dev`: chạy server có tự reload, mặc định ở http://localhost:3000
- `npm run build`: build ra `dist/`
- `npm test`: unit test (Vitest, file `*.spec.ts` đặt cạnh code)
- `npm run test:e2e`: e2e test (Vitest + supertest, thư mục `test/`)
- `npm run lint`: kiểm tra code bằng oxlint
- `npm run format`: định dạng code bằng Prettier

## Quy ước
- Code là ESM: import file nội bộ phải có đuôi `.js` (vd. `import { AppService } from './app.service.js'`).
- Test dùng Vitest (`describe`, `it`, `expect` là global), không dùng Jest.
- Cổng mặc định 3000, đổi bằng biến môi trường `PORT`.
