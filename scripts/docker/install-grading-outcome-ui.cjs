const c=require('./data-common.cjs'),{fs,path,root,workspace}=c;
require(path.join(root,'tmp/grading-ui/enhance-workspace.cjs'));
const panel=path.join(root,'tmp/grading-ui/GradingResultsPanel.tsx');fs.writeFileSync(panel,fs.readFileSync(panel,'utf8').replaceAll('user?.id','user?.accountId'));
require('./install-grading-ui.cjs');
const site=path.join(workspace,'language-center-web-frontend');fs.copyFileSync(panel,path.join(site,'src/components/Common/Evaluation/GradingResultsPanel.tsx'));
const app=path.join(workspace,'language-center-student-app');fs.copyFileSync(path.join(root,'tmp/grading-ui/GradingPublishedResult.tsx'),path.join(app,'src/features/progress/GradingPublishedResult.tsx'));
const progress=path.join(app,'src/features/progress/Progress.tsx');let text=fs.readFileSync(progress,'utf8');if(!text.includes('GradingPublishedResult')){text='import GradingPublishedResult from "./GradingPublishedResult";\n'+text;const anchor='<ClassContext id={key} />';c.assert.ok(text.includes(anchor));text=text.replace(anchor,anchor+'\n      {mode === \'grades\' && <GradingPublishedResult enrollmentId={key} />}');fs.writeFileSync(progress,text);}
console.log('Grading calculation/review UI and student published results installed.');
require(path.join(root,'tmp/grading-ui/fix-mobile-published.cjs'));
