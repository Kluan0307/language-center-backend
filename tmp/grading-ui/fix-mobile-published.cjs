const fs=require('fs'),path=require('path'),c=require('../../scripts/docker/data-common.cjs');
const target=path.join(c.workspace,'language-center-student-app/src/features/progress/Progress.tsx');let s=fs.readFileSync(target,'utf8');
if(!s.includes('schemePublished')){
 s=s.replace("  const data = rows.data;","  const data = rows.data;\n  const [schemePublished,setSchemePublished]=useState(false),[gradingRevision,setGradingRevision]=useState(0);");
 s=s.replace('rows.reload(); outcome.reload();','rows.reload(); outcome.reload(); setGradingRevision(v=>v+1);');
 s=s.replace('<GradingPublishedResult enrollmentId={key} />','<GradingPublishedResult enrollmentId={key} onPublishedChange={setSchemePublished} refreshRevision={gradingRevision} />');
 s=s.replace("{mode === 'grades' && <Card><Heading", "{mode === 'grades' && !schemePublished && <Card><Heading");
 s=s.replace('!data.content.length ? <StateView','!data.content.length && !schemePublished ? <StateView');
 fs.writeFileSync(target,s);
}
