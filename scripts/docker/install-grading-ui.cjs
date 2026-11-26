const c=require('./data-common.cjs'),{fs,path,root,workspace,assert}=c;
const site=path.join(workspace,'language-center-web-frontend'),source=path.join(root,'tmp/grading-ui');
for(const [from,to] of [['gradingService.ts','src/services/gradingService.ts'],['GradingWorkspace.tsx','src/components/Common/Evaluation/GradingWorkspace.tsx']]){fs.writeFileSync(path.join(site,to),fs.readFileSync(path.join(source,from)));}
const target=path.join(site,'src/pages/GradeManagement/index.tsx');let text=fs.readFileSync(target,'utf8');
if(!text.includes('GradingWorkspace')){text='import GradingWorkspace from "../../components/Common/Evaluation/GradingWorkspace";\n'+text;const anchor='items={[\n';assert.ok(text.includes(anchor),'Grade tab anchor missing');text=text.replace(anchor,anchor+'              {key:"schemes",label:"Đánh giá theo quy chế",children:<GradingWorkspace key={classId} classId={classId} schoolClass={classes.data?.find(c=>c.id===classId)}/>},\n');fs.writeFileSync(target,text);}
console.log('Grading workspace added to existing Grade Management tabs.');
