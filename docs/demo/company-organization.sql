-- Optional initial organizational chart; no invented Chairman/CEO assignments.
START TRANSACTION;
INSERT INTO organization_units(name,version,active) SELECT 'Trung tâm ngoại ngữ',0,1 WHERE NOT EXISTS(SELECT 1 FROM organization_units WHERE parent_id IS NULL AND name='Trung tâm ngoại ngữ');
SET @company_root=(SELECT MIN(id) FROM organization_units WHERE parent_id IS NULL AND name='Trung tâm ngoại ngữ');
INSERT INTO organization_units(name,parent_id,version,active) SELECT x.name,@company_root,0,1 FROM (SELECT 'Ban lãnh đạo' name UNION ALL SELECT 'Bộ phận đào tạo' UNION ALL SELECT 'Tuyển sinh và chăm sóc học viên' UNION ALL SELECT 'Hành chính – kế toán') x WHERE NOT EXISTS(SELECT 1 FROM organization_units u WHERE u.name=x.name AND u.parent_id=@company_root);
INSERT INTO organization_units(name,parent_id,branch_id,version,active) SELECT b.name,@company_root,b.id,0,1 FROM branches b WHERE b.status='ACTIVE' AND NOT EXISTS(SELECT 1 FROM organization_units u WHERE u.branch_id=b.id AND u.parent_id=@company_root);
INSERT INTO job_titles(name,version,active) SELECT x.name,0,1 FROM (SELECT 'Chủ tịch' name UNION ALL SELECT 'Giám đốc điều hành' UNION ALL SELECT 'Giám đốc đào tạo' UNION ALL SELECT 'Trưởng bộ phận đào tạo' UNION ALL SELECT 'Trưởng bộ phận tuyển sinh' UNION ALL SELECT 'Quản lý chi nhánh' UNION ALL SELECT 'Nhân viên tư vấn' UNION ALL SELECT 'Giáo viên' UNION ALL SELECT 'Nhân viên hành chính – kế toán') x WHERE NOT EXISTS(SELECT 1 FROM job_titles t WHERE t.name=x.name);
INSERT INTO organization_assignments(account_id,unit_id,title_id,manager_account_id,version,active)
SELECT a.id,u.id,t.id,CASE WHEN a.role='ADMIN' THEN NULL ELSE (SELECT MIN(ad.id) FROM accounts ad JOIN account_branches ab ON ab.account_id=ad.id AND ab.status=1 WHERE ad.role='ADMIN' AND ab.branch_id=u.branch_id) END,0,1
FROM accounts a JOIN account_branches b ON b.account_id=a.id AND b.status=1 JOIN organization_units u ON u.branch_id=b.branch_id AND u.parent_id=@company_root JOIN job_titles t ON t.name=CASE a.role WHEN 'ADMIN' THEN 'Quản lý chi nhánh' WHEN 'STAFF' THEN 'Nhân viên tư vấn' WHEN 'TEACHER' THEN 'Giáo viên' END
WHERE a.role IN ('ADMIN','STAFF','TEACHER') AND a.status=1 AND NOT EXISTS(SELECT 1 FROM organization_assignments x WHERE x.account_id=a.id AND x.unit_id=u.id AND x.title_id=t.id);
COMMIT;
