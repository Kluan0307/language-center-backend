const fs=require('fs'),p='teaching-service/src/main/java/com/example/teaching/publication/StudentProgressService.java';let s=fs.readFileSync(p,'utf8');
s=s.replace('Set.of("PASS","FAIL").contains(r.getOutcome())','Set.of("PASS","FAIL","ASSESSED").contains(r.getOutcome())&&gradingCurrent(r)');
if(!s.includes('boolean gradingCurrent(')){const at=' private Source owner(';s=s.replace(at,' private boolean gradingCurrent(FinalResult r){if(r.getGradingReleaseId()==null)return true;var release=gradingGuard.latest(r.getClassId());return release!=null&&release.getId().equals(r.getGradingReleaseId())&&release.getState().equals("PUBLISHED");}\n'+at);}
fs.writeFileSync(p,s);
