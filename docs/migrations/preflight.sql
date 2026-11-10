-- Read-only inspection. Select the intended database first with USE.
SELECT DATABASE() AS selected_database;
SELECT table_name, column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema=DATABASE()
  AND table_name IN ('accounts','students','courses','rooms','school_classes',
    'schedules','enrollments','attendances','tuition','payments','exams','documents','grades')
ORDER BY table_name,ordinal_position;
SELECT table_name,index_name,non_unique,GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_index
FROM information_schema.statistics
WHERE table_schema=DATABASE() AND table_name IN ('students','courses','rooms','tuition','payments')
GROUP BY table_name,index_name,non_unique ORDER BY table_name,index_name;
