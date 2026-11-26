const canonical=['GE','COMMUNICATION','IELTS','TOEIC'];
const aliases=new Map([['GE','GE'],['GENERAL ENGLISH','GE'],['TIẾNG ANH TỔNG QUÁT','GE'],['COMMUNICATION','COMMUNICATION'],['GIAO TIẾP','COMMUNICATION'],['TIẾNG ANH GIAO TIẾP','COMMUNICATION'],['IELTS','IELTS'],['TOEIC','TOEIC']]);
function decide(row){
 const legacy=aliases.get((row.legacyType||'').trim().toUpperCase())||null;
 const policy=row.policyCode&&row.policyMode!=='UNVERIFIED'?row.policyCode.toUpperCase():null;
 const published=row.websiteProgramCode?.toUpperCase()||null;
 const values=[legacy,policy,published,row.currentProgramCode?.toUpperCase()].filter(Boolean);
 if(values.some(x=>!canonical.includes(x)))return {status:'REVIEW',suggested:null,reason:'Chương trình ngoài danh mục chuẩn cần xác nhận'};
 if(new Set(values).size>1)return {status:'CONFLICT',suggested:null,reason:'Loại cũ, chính sách, website hoặc chương trình hiện tại không khớp'};
 if(row.currentProgramId)return {status:'PRESERVED',suggested:row.currentProgramCode,reason:'Giữ nguyên liên kết đã có'};
 if(policy)return {status:'AUTO',suggested:policy,reason:'Chính sách hiện hành đã xác nhận; các nguồn không mâu thuẫn'};
 if(legacy&&!row.policyId)return {status:'AUTO',suggested:legacy,reason:'Ánh xạ chính xác loại cũ; không có chính sách mâu thuẫn'};
 return {status:'REVIEW',suggested:null,reason:'Thiếu căn cứ hoặc chính sách chưa xác minh'};
}
module.exports={decide,canonical};
