package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Add independent structured records; legacy activity treatment fields are not interpreted. */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `spray_application` (`activity_id` INTEGER NOT NULL, `application_date` TEXT NOT NULL, `local_time` TEXT, `time_zone` TEXT NOT NULL, `treated_area` REAL, `area_unit` TEXT, `water_quantity` REAL, `water_unit` TEXT, `notes` TEXT, `revision` INTEGER NOT NULL, `generation` TEXT NOT NULL, PRIMARY KEY(`activity_id`), FOREIGN KEY(`activity_id`) REFERENCES `activity`(`activity_id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `spray_product` (`product_line_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `activity_id` INTEGER NOT NULL, `line_position` INTEGER NOT NULL, `product_name` TEXT NOT NULL, `quantity` REAL, `quantity_unit` TEXT, `label_reference` TEXT, FOREIGN KEY(`activity_id`) REFERENCES `spray_application`(`activity_id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_spray_product_activity_id_line_position` ON `spray_product` (`activity_id`, `line_position`)")
    }
}
