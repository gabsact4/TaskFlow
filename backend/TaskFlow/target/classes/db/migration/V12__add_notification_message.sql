-- Older V8 schemas persisted notification titles but did not have a message
-- column. Add it without replacing rows and recover text from a legacy body,
-- content, or description column when one exists.
SET @taskflow_message_column_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'notifications'
              AND column_name = 'message'
        ),
        'SELECT 1',
        'ALTER TABLE notifications ADD COLUMN message VARCHAR(500) NULL'
    )
);
PREPARE taskflow_message_column_stmt FROM @taskflow_message_column_sql;
EXECUTE taskflow_message_column_stmt;
DEALLOCATE PREPARE taskflow_message_column_stmt;

SET @taskflow_legacy_message_column = (
    SELECT CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'body') THEN 'body'
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'content') THEN 'content'
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'description') THEN 'description'
        ELSE NULL
    END
);

SET @taskflow_message_backfill_sql = IF(
    @taskflow_legacy_message_column IS NULL,
    'UPDATE notifications SET message = LEFT(title, 500) WHERE message IS NULL',
    CONCAT(
        'UPDATE notifications SET message = LEFT(COALESCE(`',
        @taskflow_legacy_message_column,
        '`, title), 500) WHERE message IS NULL'
    )
);
PREPARE taskflow_message_backfill_stmt FROM @taskflow_message_backfill_sql;
EXECUTE taskflow_message_backfill_stmt;
DEALLOCATE PREPARE taskflow_message_backfill_stmt;

ALTER TABLE notifications MODIFY COLUMN message VARCHAR(500) NOT NULL;
