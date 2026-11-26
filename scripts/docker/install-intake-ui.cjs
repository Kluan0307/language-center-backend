const {fs,path,root,workspace}=require('./data-common.cjs');const web=path.join(workspace,'language-center-web-frontend');const source=path.join(root,'tmp/tn34');
for(const f of ['ConvertToStudentModal','LeadPreferencesModal'])fs.copyFileSync(path.join(source,f+'.tsx'),path.join(web,'src/pages/Leads',f+'.tsx'));
fs.copyFileSync(path.join(source,'leadIntakeService.ts'),path.join(web,'src/services/leadIntakeService.ts'));
function edit(file,fn){const p=path.join(web,file);fs.writeFileSync(p,fn(fs.readFileSync(p,'utf8')));}
edit('src/types/admissions.ts',s=>s.includes('preferredClassName')?s:s.replace('  version:number;nextContactAt:string|null;overdue:boolean;','  preferredClassId?:number|null;preferredClassName?:string|null;preferredTime?:string|null;\n  version:number;nextContactAt:string|null;overdue:boolean;'));
edit('src/pages/Leads/LeadHistory.tsx',s=>s.replace(/(?:PREFERENCE_CHANGED:'Cập nhật nhu cầu',INTAKE_CONFIRMED:'Tiếp nhận học viên',)+CREATED:'Tiếp nhận'/,"PREFERENCE_CHANGED:'Cập nhật nhu cầu',INTAKE_CONFIRMED:'Tiếp nhận học viên',CREATED:'Tiếp nhận'").replace(/const labels:Record<string,string>=\{CREATED:/,"const labels:Record<string,string>={PREFERENCE_CHANGED:'Cập nhật nhu cầu',INTAKE_CONFIRMED:'Tiếp nhận học viên',CREATED:"));
edit('src/pages/Leads/LeadsPage.tsx',s=>{
 if(!s.includes("import LeadPreferencesModal"))s="import LeadPreferencesModal from './LeadPreferencesModal';\n"+s;
 if(!s.includes('[preference,setPreference]'))s=s.replace('  const [convert, setConvert]', '  const [preference,setPreference]=useState<LeadDto|null>(null);\n  const [convert, setConvert]');
 s=s.replace('setFormState(null); setCare(null);setAssignment(null); setConvert(null);','setFormState(null); setCare(null);setAssignment(null); setConvert(null);setPreference(null);');
 s=s.replace("'Đã chuyển thành học viên. Hãy bổ sung hồ sơ và ghi danh lớp.'","'Đã lưu kết quả tiếp nhận học viên.'").replace('label: "Chuyển học viên"','label: "Tiếp nhận học viên"');
 if(!s.includes('key:"preference"'))s=s.replace('key: "action-2", label: "Chăm sóc", icon: <PhoneCall size={14}/>, onClick: () => setCare(lead)','key: "action-2", label: "Chăm sóc", icon: <PhoneCall size={14}/>, onClick: () => setCare(lead)\n        },{key:"preference",label:"Nhu cầu lớp",onClick:()=>setPreference(lead)');
 if(!s.includes("key:'preferredClass'"))s=s.replace("{ key: 'level', label: 'Trình độ mong muốn'", "{key:'preferredClass',label:'Lớp mong muốn',children:detail.data.preferredClassName||'Chưa chọn'},\n          {key:'preferredTime',label:'Khung giờ phù hợp',children:detail.data.preferredTime||'Chưa cung cấp'},\n          { key: 'level', label: 'Trình độ mong muốn'");
 if(!s.includes('{preference&&'))s=s.replace('    {convert && <ConvertToStudentModal','    {preference&&<LeadPreferencesModal lead={preference} onClose={()=>setPreference(null)} onSaved={saved}/>}\n    {convert && <ConvertToStudentModal');
 if(!s.includes('function hasPendingIntake'))s=s.replace('  const actions =',"  function hasPendingIntake(lead:LeadDto){try{return !!sessionStorage.getItem('lc:intake:'+user?.accountId+':'+lead.id);}catch{return false;}}\n  const actions =");
 s=s.replace('canWrite(lead) && canConvertLead(lead.status)','canWrite(lead) && (canConvertLead(lead.status)||hasPendingIntake(lead))').replace('label: "Tiếp nhận học viên", icon:', 'label: hasPendingIntake(lead)?"Kiểm tra tiếp nhận":"Tiếp nhận học viên", icon:');
 return s;
});
console.log('TN3 intake wizard and preferences UI installed.');
