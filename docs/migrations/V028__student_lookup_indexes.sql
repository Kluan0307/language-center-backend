-- Apply once per database after checking information_schema.statistics.
-- No application rows are changed. Docker Hibernate remains validate.
CREATE INDEX idx_student_branch_id ON students(branch_id,id);
CREATE INDEX idx_tuition_student_due ON tuition(student_id,due_date,id);
-- Rollback: DROP INDEX idx_student_branch_id ON students;
-- Rollback: DROP INDEX idx_tuition_student_due ON tuition;
