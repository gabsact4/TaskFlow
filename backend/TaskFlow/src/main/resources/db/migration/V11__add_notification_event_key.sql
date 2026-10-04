-- Earlier V8 variants created notifications without event_key. Add it without
-- dropping or recreating existing notification rows; use each row ID for a
-- stable unique value so old notifications remain readable.
SET @taskflow_event_key_column_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'notifications'
              AND column_name = 'event_key'
        ),
        'SELECT 1',
        'ALTER TABLE notifications ADD COLUMN event_key VARCHAR(200) NULL'
    )
);
PREPARE taskflow_event_key_column_stmt FROM @taskflow_event_key_column_sql;
EXECUTE taskflow_event_key_column_stmt;
DEALLOCATE PREPARE taskflow_event_key_column_stmt;

UPDATE notifications
SET event_key = CONCAT('legacy:', id)
WHERE event_key IS NULL OR event_key = '';

SET @taskflow_event_key_index_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'notifications'
              AND index_name = 'uk_notifications_event_key'
        ),
        'SELECT 1',
        'ALTER TABLE notifications ADD CONSTRAINT uk_notifications_event_key UNIQUE (event_key)'
    )
);
PREPARE taskflow_event_key_index_stmt FROM @taskflow_event_key_index_sql;
EXECUTE taskflow_event_key_index_stmt;
DEALLOCATE PREPARE taskflow_event_key_index_stmt;

ALTER TABLE notifications MODIFY COLUMN event_key VARCHAR(200) NOT NULL;
