const fs=require('fs'),p=require('path').resolve(__dirname,'../../scripts/docker/install-grading-transition-ui.cjs');let s=fs.readFileSync(p,'utf8');
s=s.replace(".replace('\"&view=final&enrollmentId=\"','(f.gradingReleaseId ? \"&view=schemes&enrollmentId=\" : \"&view=final&enrollmentId=\")')",'');
s=s.replace('.replace("outcome.data.outcome === \'PASS\' ? \'Đạt\' : \'Chưa đạt\'", \'', '.replace("outcome.data.outcome === \'PASS\' ? \'Đạt\' : \'Chưa đạt\'", \'');
s+='\nedit(app,\'src/features/progress/Progress.tsx\',s=>s.replace(/(?:outcome\\.data\\.outcome === \'ASSESSED\' \\? \'Đã hoàn thành đánh giá\' : ){2,}/g,"outcome.data.outcome === \'ASSESSED\' ? \'Đã hoàn thành đánh giá\' : "));\n';
fs.writeFileSync(p,s);
