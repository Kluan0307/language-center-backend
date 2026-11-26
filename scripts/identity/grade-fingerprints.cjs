const crypto=require('node:crypto'),assert=require('node:assert/strict');
const hash=value=>crypto.createHash('sha256').update(typeof value==='string'?value:JSON.stringify(value)).digest('hex');
// Match Jackson's BigDecimal.stripTrailingZeros() token, including negative scale.
// e.g. Java writes 10.00 as 1E+1; JSON.stringify(10) writes 10 and hashes differently.
function decimal(value){let text=String(value);assert.match(text,/^-?\d+(?:\.\d+)?$/);text=text.replace(/(\.\d*?)0+$/,'$1').replace(/\.$/,'');
 const negative=text.startsWith('-');if(negative)text=text.slice(1);text=text.replace(/^0+(?=\d)/,'');if(Number(text)===0)return '0';
 if(!text.includes('.')&&text.endsWith('0')){const digits=text.replace(/0+$/,'');return (negative?'-':'')+digits[0]+(digits.length>1?'.'+digits.slice(1):'')+'E+'+(text.length-1);}
 return (negative?'-':'')+text;
}
function draftFingerprint({name,type,date,maxScore,weight,frozen,candidates,grades}){
 const gs=grades.map(g=>'['+[JSON.stringify(g[0]),decimal(g[1]),JSON.stringify(g[2]),JSON.stringify(g[3])].join(',')+']').join(',');
 return hash('['+[JSON.stringify(name),JSON.stringify(type),JSON.stringify(date),decimal(maxScore),decimal(weight),JSON.stringify(frozen),JSON.stringify(candidates),'['+gs+']'].join(',')+']');
}
function fromDatabase(db,query,examId){
 const json=sql=>JSON.parse(query(db,sql));
 const e=json("SELECT JSON_OBJECT('id',id,'name',name,'type',exam_type,'date',exam_date,'maxScore',CAST(max_score AS CHAR),'weight',CAST(weight AS CHAR),'frozen',IF(roster_frozen_at IS NOT NULL,TRUE,FALSE)) FROM exams WHERE id="+Number(examId));
 const candidates=json('SELECT JSON_ARRAYAGG(JSON_ARRAY(student_id,enrollment_id,source)) FROM (SELECT * FROM exam_candidates WHERE exam_id='+Number(examId)+' ORDER BY student_id LIMIT 100000) c');
 const grades=json('SELECT JSON_ARRAYAGG(JSON_ARRAY(student_id,CAST(score AS CHAR),student_comment,version)) FROM (SELECT * FROM grades WHERE exam_id='+Number(examId)+' ORDER BY student_id LIMIT 100000) g');
 return draftFingerprint({...e,frozen:Boolean(e.frozen),candidates,grades});
}
module.exports={decimal,draftFingerprint,fromDatabase,hash};
