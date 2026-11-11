-- AlterTable
ALTER TABLE `users` ADD COLUMN `google_sub` VARCHAR(255) NULL,
    MODIFY `password_hash` VARCHAR(255) NULL;

-- CreateIndex
CREATE UNIQUE INDEX `users_google_sub_key` ON `users`(`google_sub`);
