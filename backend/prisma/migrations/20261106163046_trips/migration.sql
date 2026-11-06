-- CreateTable
CREATE TABLE `place_interests` (
    `place_id` INTEGER NOT NULL,
    `interest` ENUM('FOOD', 'BEACH', 'SCENERY', 'RELAX', 'FUN', 'PHOTO', 'CULTURE', 'EXPLORE') NOT NULL,

    PRIMARY KEY (`place_id`, `interest`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `trips` (
    `id` INTEGER NOT NULL AUTO_INCREMENT,
    `owner_id` INTEGER NOT NULL,
    `type` ENUM('SOLO', 'GROUP') NOT NULL,
    `name` VARCHAR(100) NULL,
    `city` VARCHAR(100) NOT NULL,
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `day_start_minute` INTEGER NOT NULL DEFAULT 480,
    `day_end_minute` INTEGER NOT NULL DEFAULT 1260,
    `gap_minutes` INTEGER NOT NULL DEFAULT 30,
    `lodging_mode` ENUM('HOTEL', 'SELF') NOT NULL DEFAULT 'SELF',
    `status` ENUM('DRAFT', 'PLANNED') NOT NULL DEFAULT 'DRAFT',
    `invite_code` VARCHAR(8) NULL,
    `vote_deadline` DATETIME(3) NULL,
    `planned_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL,

    UNIQUE INDEX `trips_invite_code_key`(`invite_code`),
    INDEX `trips_owner_id_idx`(`owner_id`),
    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `trip_members` (
    `id` INTEGER NOT NULL AUTO_INCREMENT,
    `trip_id` INTEGER NOT NULL,
    `user_id` INTEGER NOT NULL,
    `role` ENUM('OWNER', 'MEMBER') NOT NULL DEFAULT 'MEMBER',
    `budget` INTEGER NOT NULL DEFAULT 0,
    `lodging_share` INTEGER NOT NULL DEFAULT 0,
    `satisfaction` INTEGER NULL,
    `ballot_sent_at` DATETIME(3) NULL,
    `joined_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    INDEX `trip_members_user_id_idx`(`user_id`),
    UNIQUE INDEX `trip_members_trip_id_user_id_key`(`trip_id`, `user_id`),
    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `member_interests` (
    `member_id` INTEGER NOT NULL,
    `interest` ENUM('FOOD', 'BEACH', 'SCENERY', 'RELAX', 'FUN', 'PHOTO', 'CULTURE', 'EXPLORE') NOT NULL,

    PRIMARY KEY (`member_id`, `interest`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `trip_lodgings` (
    `trip_id` INTEGER NOT NULL,
    `place_id` INTEGER NOT NULL,
    `check_in` DATE NOT NULL,
    `check_out` DATE NOT NULL,
    `rooms` INTEGER NOT NULL,
    `guests_per_room` INTEGER NOT NULL,
    `price_per_night` INTEGER NOT NULL,
    `price_source` ENUM('PARTNER_API', 'GOOGLE_API', 'ESTIMATED') NOT NULL,
    `price_provider` VARCHAR(100) NOT NULL,
    `priced_at` DATETIME(3) NOT NULL,

    INDEX `trip_lodgings_place_id_idx`(`place_id`),
    PRIMARY KEY (`trip_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `trip_stops` (
    `id` INTEGER NOT NULL AUTO_INCREMENT,
    `trip_id` INTEGER NOT NULL,
    `place_id` INTEGER NOT NULL,
    `day` INTEGER NOT NULL,
    `position` INTEGER NOT NULL,
    `start_minute` INTEGER NOT NULL,
    `travel_minutes` INTEGER NOT NULL,
    `visit_minutes` INTEGER NOT NULL,
    `cost_per_person` INTEGER NOT NULL,
    `price_source` ENUM('PARTNER_API', 'GOOGLE_API', 'ESTIMATED') NOT NULL,
    `price_provider` VARCHAR(100) NOT NULL,

    INDEX `trip_stops_place_id_idx`(`place_id`),
    UNIQUE INDEX `trip_stops_trip_id_day_position_key`(`trip_id`, `day`, `position`),
    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `stop_participants` (
    `stop_id` INTEGER NOT NULL,
    `member_id` INTEGER NOT NULL,
    `cost` INTEGER NOT NULL,

    INDEX `stop_participants_member_id_idx`(`member_id`),
    PRIMARY KEY (`stop_id`, `member_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `vote_options` (
    `id` INTEGER NOT NULL AUTO_INCREMENT,
    `trip_id` INTEGER NOT NULL,
    `place_id` INTEGER NOT NULL,
    `added_by_id` INTEGER NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    INDEX `vote_options_place_id_idx`(`place_id`),
    INDEX `vote_options_added_by_id_idx`(`added_by_id`),
    UNIQUE INDEX `vote_options_trip_id_place_id_key`(`trip_id`, `place_id`),
    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `votes` (
    `option_id` INTEGER NOT NULL,
    `member_id` INTEGER NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    INDEX `votes_member_id_idx`(`member_id`),
    PRIMARY KEY (`option_id`, `member_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- AddForeignKey
ALTER TABLE `place_interests` ADD CONSTRAINT `place_interests_place_id_fkey` FOREIGN KEY (`place_id`) REFERENCES `places`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `trips` ADD CONSTRAINT `trips_owner_id_fkey` FOREIGN KEY (`owner_id`) REFERENCES `users`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `trip_members` ADD CONSTRAINT `trip_members_trip_id_fkey` FOREIGN KEY (`trip_id`) REFERENCES `trips`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `trip_members` ADD CONSTRAINT `trip_members_user_id_fkey` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `member_interests` ADD CONSTRAINT `member_interests_member_id_fkey` FOREIGN KEY (`member_id`) REFERENCES `trip_members`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `trip_lodgings` ADD CONSTRAINT `trip_lodgings_trip_id_fkey` FOREIGN KEY (`trip_id`) REFERENCES `trips`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `trip_lodgings` ADD CONSTRAINT `trip_lodgings_place_id_fkey` FOREIGN KEY (`place_id`) REFERENCES `places`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `trip_stops` ADD CONSTRAINT `trip_stops_trip_id_fkey` FOREIGN KEY (`trip_id`) REFERENCES `trips`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `trip_stops` ADD CONSTRAINT `trip_stops_place_id_fkey` FOREIGN KEY (`place_id`) REFERENCES `places`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `stop_participants` ADD CONSTRAINT `stop_participants_stop_id_fkey` FOREIGN KEY (`stop_id`) REFERENCES `trip_stops`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `stop_participants` ADD CONSTRAINT `stop_participants_member_id_fkey` FOREIGN KEY (`member_id`) REFERENCES `trip_members`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `vote_options` ADD CONSTRAINT `vote_options_trip_id_fkey` FOREIGN KEY (`trip_id`) REFERENCES `trips`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `vote_options` ADD CONSTRAINT `vote_options_place_id_fkey` FOREIGN KEY (`place_id`) REFERENCES `places`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `vote_options` ADD CONSTRAINT `vote_options_added_by_id_fkey` FOREIGN KEY (`added_by_id`) REFERENCES `trip_members`(`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `votes` ADD CONSTRAINT `votes_option_id_fkey` FOREIGN KEY (`option_id`) REFERENCES `vote_options`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `votes` ADD CONSTRAINT `votes_member_id_fkey` FOREIGN KEY (`member_id`) REFERENCES `trip_members`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- Added by hand: Prisma schema cannot express CHECK constraints.
ALTER TABLE `trips`
    ADD CONSTRAINT `trips_dates_check` CHECK (`end_date` >= `start_date`),
    ADD CONSTRAINT `trips_day_window_check`
        CHECK (`day_start_minute` >= 0 AND `day_end_minute` <= 1440 AND `day_start_minute` < `day_end_minute`),
    ADD CONSTRAINT `trips_gap_check` CHECK (`gap_minutes` BETWEEN 0 AND 240);

ALTER TABLE `trip_members`
    ADD CONSTRAINT `trip_members_money_check` CHECK (`budget` >= 0 AND `lodging_share` >= 0),
    ADD CONSTRAINT `trip_members_satisfaction_check`
        CHECK (`satisfaction` IS NULL OR `satisfaction` BETWEEN 0 AND 100);

ALTER TABLE `trip_lodgings`
    ADD CONSTRAINT `trip_lodgings_stay_check`
        CHECK (`check_out` > `check_in` AND `rooms` >= 1 AND `guests_per_room` >= 1 AND `price_per_night` >= 0);

ALTER TABLE `trip_stops`
    ADD CONSTRAINT `trip_stops_order_check`
        CHECK (`day` >= 1 AND `position` >= 1 AND `start_minute` >= 0 AND `travel_minutes` >= 0
            AND `visit_minutes` > 0 AND `cost_per_person` >= 0);

ALTER TABLE `stop_participants`
    ADD CONSTRAINT `stop_participants_cost_check` CHECK (`cost` >= 0);
