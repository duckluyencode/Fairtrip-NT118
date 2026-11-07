-- CreateTable
CREATE TABLE `places` (
    `id` INTEGER NOT NULL AUTO_INCREMENT,
    `source` ENUM('CATALOG', 'GOOGLE') NOT NULL DEFAULT 'CATALOG',
    `slug` VARCHAR(100) NULL,
    `google_place_id` VARCHAR(255) NULL,
    `name` VARCHAR(200) NULL,
    `area` VARCHAR(200) NULL,
    `city` VARCHAR(100) NULL,
    `category` ENUM('FOOD', 'FUN', 'SCENIC') NULL,
    `lat` DOUBLE NULL,
    `lng` DOUBLE NULL,
    `rating` DOUBLE NULL,
    `rating_count` INTEGER NULL,
    `open_minute` INTEGER NULL,
    `close_minute` INTEGER NULL,
    `description` TEXT NULL,
    `image_url` VARCHAR(2048) NULL,
    `visit_minutes` INTEGER NULL,
    `price_min` INTEGER NULL,
    `price_max` INTEGER NULL,
    `price_source` ENUM('PARTNER_API', 'GOOGLE_API', 'ESTIMATED') NULL,
    `price_provider` VARCHAR(100) NULL,
    `price_updated_at` DATETIME(3) NULL,
    `parent_id` INTEGER NULL,
    `search_text` VARCHAR(600) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL,

    UNIQUE INDEX `places_slug_key`(`slug`),
    UNIQUE INDEX `places_google_place_id_key`(`google_place_id`),
    INDEX `places_source_city_idx`(`source`, `city`),
    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `place_categories` (
    `place_id` INTEGER NOT NULL,
    `category` ENUM('FOOD', 'FUN', 'SCENIC') NOT NULL,

    INDEX `place_categories_category_idx`(`category`),
    PRIMARY KEY (`place_id`, `category`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `search_history` (
    `id` INTEGER NOT NULL AUTO_INCREMENT,
    `user_id` INTEGER NOT NULL,
    `query` VARCHAR(100) NOT NULL,
    `normalized_query` VARCHAR(100) NOT NULL,
    `category` ENUM('FOOD', 'FUN', 'SCENIC') NULL,
    `searched_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    INDEX `search_history_user_id_searched_at_idx`(`user_id`, `searched_at`),
    UNIQUE INDEX `search_history_user_id_normalized_query_key`(`user_id`, `normalized_query`),
    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `favorites` (
    `user_id` INTEGER NOT NULL,
    `place_id` INTEGER NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    INDEX `favorites_place_id_idx`(`place_id`),
    PRIMARY KEY (`user_id`, `place_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- AddForeignKey
ALTER TABLE `places` ADD CONSTRAINT `places_parent_id_fkey` FOREIGN KEY (`parent_id`) REFERENCES `places`(`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `place_categories` ADD CONSTRAINT `place_categories_place_id_fkey` FOREIGN KEY (`place_id`) REFERENCES `places`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `search_history` ADD CONSTRAINT `search_history_user_id_fkey` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `favorites` ADD CONSTRAINT `favorites_user_id_fkey` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `favorites` ADD CONSTRAINT `favorites_place_id_fkey` FOREIGN KEY (`place_id`) REFERENCES `places`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- Added by hand: Prisma schema cannot express CHECK constraints.
-- MySQL forbids CHECK on columns with foreign key actions, so "a catalog place has a price
-- or a parent ticketed area" is enforced by the seed script instead.
ALTER TABLE `places`
    ADD CONSTRAINT `places_google_has_id_check`
        CHECK (`source` <> 'GOOGLE' OR `google_place_id` IS NOT NULL),
    ADD CONSTRAINT `places_catalog_complete_check`
        CHECK (`source` <> 'CATALOG' OR (`slug` IS NOT NULL AND `name` IS NOT NULL
            AND `city` IS NOT NULL AND `category` IS NOT NULL AND `lat` IS NOT NULL
            AND `lng` IS NOT NULL AND `visit_minutes` IS NOT NULL AND `search_text` IS NOT NULL)),
    ADD CONSTRAINT `places_price_range_check`
        CHECK ((`price_min` IS NULL AND `price_max` IS NULL)
            OR (`price_min` >= 0 AND `price_max` >= `price_min`)),
    ADD CONSTRAINT `places_hours_check`
        CHECK ((`open_minute` IS NULL AND `close_minute` IS NULL)
            OR (`open_minute` BETWEEN 0 AND 1439 AND `close_minute` BETWEEN 0 AND 1439));
