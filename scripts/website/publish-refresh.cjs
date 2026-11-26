const {fs,path,root,dir,query,dump,read,assert}=require('../identity/refresh-common.cjs');
const {withServices}=require('./local-services.cjs');
const slug=v=>v.normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'');
const programSlugs={GE:'tieng-anh-tong-quat',COMMUNICATION:'tieng-anh-giao-tiep',IELTS:'ielts',TOEIC:'toeic'};
const copy={GE:['Xây nền tảng tiếng Anh qua nghe, nói, đọc và viết. Chọn khóa theo giai đoạn học và mục tiêu của bạn.','Từ vựng và cấu trúc cơ bản\nNghe hiểu và phát âm\nThực hành trong ngữ cảnh'],COMMUNICATION:['Thực hành tiếng Anh trong các tình huống học tập, công việc và đời sống. Có lộ trình cơ bản và nâng cao.','Phát âm và phản xạ\nHội thoại thực tế\nTrình bày ý kiến'],IELTS:['Làm quen cấu trúc bài thi và luyện tập bốn kỹ năng theo trình độ đầu vào. Mục tiêu từng khóa là định hướng học tập, không phải cam kết điểm thi.','Nghe và đọc theo dạng bài\nViết có cấu trúc\nThực hành nói và phản hồi'],TOEIC:['Rèn luyện kỹ năng nghe và đọc, từ vựng cùng cách tiếp cận các dạng bài TOEIC. Học theo mức nền tảng và mục tiêu phù hợp.','Nghe hiểu trong công việc\nĐọc hiểu và từ vựng\nLuyện tập dạng bài TOEIC']};
function recipe(db='language_center_db'){
 const programs=read('programs',['id','code','name'],db),branches=read('branches',['id','code','name'],db),courses=read('courses',['id','code','name','program_id','branch_id','expected_minutes'],db),classes=read('school_classes',['id','code','name','course_id','branch_id','start_date','status','current_size','max_size'],db),teachers=read('teachers',['id','account_id','full_name'],db);
 const targets=[];
 for(const p of programs.filter(p=>programSlugs[p.code]))targets.push({kind:'PROGRAM',sourceId:p.id,slug:programSlugs[p.code],summary:copy[p.code][0],topics:copy[p.code][1],outcomes:'Phát triển kỹ năng theo mục tiêu và trình độ của từng khóa.'});
 for(const b of branches)targets.push({kind:'BRANCH',sourceId:b.id,slug:slug(b.code),summary:'Khám phá các khóa học và lịch tuyển sinh tại '+b.name+'. Gửi yêu cầu để được tư vấn nơi học và thời gian phù hợp.'});
 for(const c of courses){const p=programs.find(p=>p.id===c.program_id);assert.ok(p&&copy[p.code]);targets.push({kind:'COURSE',sourceId:c.id,slug:slug(c.code),programId:p.id,showTuition:true,summary:c.name+' gồm 8 buổi học, tổng '+c.expected_minutes+' phút. Nội dung và điều kiện đầu vào được đối chiếu với hồ sơ khóa học.',outcomes:'Thực hành kiến thức của khóa học; nhận phản hồi từ giáo viên và theo dõi kết quả kiểm tra nội bộ.',topics:copy[p.code][1]});}
 for(const c of classes.filter(c=>c.status==='UPCOMING'&&c.current_size<c.max_size&&c.start_date>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Ho_Chi_Minh'}).format(new Date())))targets.push({kind:'CLASS',sourceId:c.id,slug:slug(c.code),summary:c.name+' đang tiếp nhận nhu cầu tư vấn. Xem lịch dự kiến và điều kiện của khóa trước khi đăng ký.'});
 return {targets,teacherTargets:teachers.map(t=>({teacherId:t.id,slug:slug('giao-vien-'+t.id),biography:t.full_name+' tham gia giảng dạy các lớp được phân công tại trung tâm. Học viên thực hành theo nội dung khóa học và nhận phản hồi trong quá trình học.',teachingFocus:'Thực hành kỹ năng tiếng Anh theo mục tiêu khóa học',showQualification:false})),programs,branches,courses,classes};
}
async function publish(api,db='language_center_db'){
 const user=await api.login('superadmin'),token=user.token,plan=recipe(db),results=[];
 for(const item of plan.targets){const old=await api.request('academic','/website/publications/'+item.kind+'?size=100',{token});assert.equal(old.status,200);
  const current=old.data.find(x=>x.sourceId===item.sourceId);
  const body={sourceId:item.sourceId,version:current?.version||0,state:'PUBLISHED',showTuition:false,audience:null,outcomes:null,topics:null,imageUrl:null,openingHours:null,mapUrl:null,programId:null,...item};delete body.kind;
  if(current){assert.equal(current.slug,item.slug,'Existing public URL differs; do not overwrite');if(Object.entries(body).every(([key,value])=>key==='version'||(current[key]??null)===(value??null))){results.push({kind:item.kind,slug:item.slug,status:'unchanged'});continue;}}
  const saved=await api.request('academic','/website/publications/'+item.kind,{token,method:'PUT',body});assert.equal(saved.status,200,'Publish '+item.kind+'/'+item.slug+': '+JSON.stringify(saved.data));results.push({kind:item.kind,slug:item.slug,status:'published'});
 }
 const existing=await api.request('academic','/website/teachers?size=100',{token});assert.equal(existing.status,200);
 for(const item of plan.teacherTargets){const old=existing.data.find(x=>x.teacherId===item.teacherId),body={version:old?.version||0,state:'PUBLISHED',imageUrl:null,...item};
  if(old){assert.equal(old.slug,item.slug);if(Object.entries(body).every(([key,value])=>key==='version'||(old[key]??null)===(value??null))){results.push({kind:'TEACHER',slug:item.slug,status:'unchanged'});continue;}}
  const saved=await api.request('academic','/website/teachers',{token,method:'PUT',body});assert.equal(saved.status,200,'Publish teacher '+item.slug+': '+JSON.stringify(saved.data));results.push({kind:'TEACHER',slug:item.slug,status:'published'});
 }
 return {results,counts:{programs:4,branches:plan.branches.length,courses:plan.courses.length,classes:plan.targets.filter(t=>t.kind==='CLASS').length,teachers:plan.teacherTargets.length}};
}
if(require.main===module){(async()=>{
 const plan=recipe();fs.writeFileSync(path.join(root,'docs/course-classification/d5-publication-plan.json'),JSON.stringify({programSlugs,targets:plan.targets,teacherTargets:plan.teacherTargets,scope:'local demonstration data; no exam-score guarantees or unreviewed qualifications'},null,2));
 if(!process.argv.includes('--apply-local'))return console.log(JSON.stringify({status:'plan-only',publications:plan.targets.length,teachers:plan.teacherTargets.length}));
 const folder=path.join(dir,'publish-'+Date.now());fs.mkdirSync(folder);fs.writeFileSync(path.join(folder,'before.sql'),dump());
 const result=await withServices({folder},api=>publish(api));
 fs.writeFileSync(path.join(root,'docs/course-classification/d5-publication-report.json'),JSON.stringify({phase:'D5',status:'published-local',observedAt:new Date().toISOString(),via:'authenticated CMS APIs',backup:'private storage/data-refresh',...result},null,2));console.log(JSON.stringify({phase:'D5',status:'published-local',counts:result.counts}));
})().catch(e=>{console.error(e.message);process.exitCode=1;});}
module.exports={recipe,publish,programSlugs};
