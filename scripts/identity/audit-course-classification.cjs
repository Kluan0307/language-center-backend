// Read-only local audit. No credentials, student details or data changes in the report.
const fs = require('node:fs'), path = require('node:path');
const { query, root } = require('./database.cjs');
const sql = `START TRANSACTION READ ONLY;
SELECT JSON_OBJECT(
 'courses', (SELECT COUNT(*) FROM courses),
 'coursesWithProgram', (SELECT COUNT(*) FROM courses WHERE program_id IS NOT NULL),
 'coursesWithoutAudiences', (SELECT COUNT(*) FROM courses c WHERE NOT EXISTS (SELECT 1 FROM course_audiences ca WHERE ca.course_id=c.id)),
 'multiAudienceCourses', (SELECT COUNT(*) FROM (SELECT course_id FROM course_audiences GROUP BY course_id HAVING COUNT(*)>1) x),
 'legacyTypeValues', (SELECT COUNT(*) FROM courses WHERE type IS NOT NULL AND type<>''),
 'unknownProgramAssignments', (SELECT COUNT(*) FROM courses c LEFT JOIN programs p ON p.id=c.program_id WHERE c.program_id IS NOT NULL AND (p.id IS NULL OR p.code NOT IN ('GE','COMMUNICATION','IELTS','TOEIC'))),
 'unknownAudienceAssignments', (SELECT COUNT(*) FROM course_audiences ca LEFT JOIN audience_groups a ON a.id=ca.audience_group_id WHERE a.id IS NULL OR a.code NOT IN ('PRESCHOOL','PRIMARY','SECONDARY','HIGH_SCHOOL','ADULT')),
 'orphanAudienceCourses', (SELECT COUNT(*) FROM course_audiences ca LEFT JOIN courses c ON c.id=ca.course_id WHERE c.id IS NULL),
 'duplicateAudiencePairs', (SELECT COUNT(*) FROM (SELECT course_id,audience_group_id FROM course_audiences GROUP BY course_id,audience_group_id HAVING COUNT(*)>1) x),
 'latestPolicyProgramMismatches', (SELECT COUNT(*) FROM courses c JOIN course_admission_policies ap ON ap.course_id=c.id AND ap.revision=(SELECT MAX(ap2.revision) FROM course_admission_policies ap2 WHERE ap2.course_id=c.id) WHERE c.program_id IS NOT NULL AND ap.program_id<>c.program_id),
 'publicationProgramMismatches', (SELECT COUNT(*) FROM website_publications wp JOIN courses c ON wp.source_id=c.id WHERE wp.kind='COURSE' AND c.program_id IS NOT NULL AND (wp.program_id IS NULL OR wp.program_id<>c.program_id)),
 'programCatalog', (SELECT JSON_ARRAYAGG(JSON_OBJECT('code',code,'active',active+0)) FROM programs),
 'audienceCatalog', (SELECT JSON_ARRAYAGG(JSON_OBJECT('code',code,'active',active+0)) FROM audience_groups)
);
ROLLBACK;`;
const data = JSON.parse(query('language_center_db', sql));
const integrityKeys = ['unknownProgramAssignments','unknownAudienceAssignments','orphanAudienceCourses','duplicateAudiencePairs','latestPolicyProgramMismatches','publicationProgramMismatches'];
const result = { readOnly: true, database: 'local language_center_db', observedAt: new Date().toISOString(),
  integrityPassed: integrityKeys.every(key => data[key] === 0), data,
  needsAudienceReview: data.coursesWithoutAudiences > 0, recordsModified: false };
const output = path.join(root, 'docs/course-classification', process.argv.includes('--d4') ? 'd4-classification-audit.json' : 'g8-local-audit.json');
fs.writeFileSync(output, JSON.stringify(result, null, 2) + '\n');
console.log(JSON.stringify(result, null, 2));
