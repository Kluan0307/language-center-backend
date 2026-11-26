import {useSession} from '../../providers/SessionProvider';
import {shareDocument} from '../documents/downloads';
import {Text,View} from 'react-native';
import {useEffect,useState,useRef} from 'react';
import {Button,Card,Heading,InlineNotice,textStyles} from '../../components/ui';
import {Badge,Disclosure} from '../../components/Details';
import {useStudentQuery} from '../learning/useStudentQuery';
import {displayDate} from '../learning/utils';
type Attempt={activityId:number;name:string;date:string;status:string;score:number|null;rawScore:number|null;conversionId:number|null;level:string|null;comment:string|null;selected:boolean};
type Item={rootId:number;activityId:number;name:string;date:string;status:string;score:number|null;rawScore:number|null;conversionId:number|null;level:string|null;comment:string|null;attempts:number[];history?:Attempt[]|null};
type Part={code:string;name:string;score:number|null;maxScore:number|null;weight:number|null;level:string|null;status:string;assessments:Item[]};
type Published={state:string;revision:number|null;publishedAt:string|null;templateName:string|null;result:{score:number|null;unit:string;academicStatus:string;completionStatus:string;attendance:{percent:number|null;status:string};components:Part[];reasons:string[]}|null;rules:string[]};
const labels:Record<string,string>={PASS:'Đạt',FAIL:'Chưa đạt',RECORDED:'Đã ghi nhận',INSUFFICIENT:'Chưa đủ dữ liệu',IN_PROGRESS:'Đang học',ATTENDANCE_NOT_MET:'Chưa đạt chuyên cần',NEEDS_SUPPORT:'Cần hỗ trợ',DEVELOPING:'Đang phát triển',MEETS_EXPECTATIONS:'Đạt yêu cầu',EXEMPT:'Được miễn',INCOMPLETE:'Chưa đủ kết quả',GRADED:'Đã đánh giá',MISSING:'Chưa nhập',ABSENT:'Vắng',NOT_SUBMITTED:'Chưa nộp',NOT_YET_DUE:'Chưa đến ngày'};
const number=(n:number|null)=>n===null?'—':String(Math.round(n*100)/100);
export default function GradingPublishedResult({enrollmentId,onPublishedChange,refreshRevision}:{enrollmentId:string;onPublishedChange:(value:boolean)=>void;refreshRevision:number}){
 const {api,identity}=useSession();const [exporting,setExporting]=useState(false);const [exportError,setExportError]=useState('');const active=useRef(0);
 useEffect(()=>{active.current++;return()=>{active.current++;};},[enrollmentId,identity?.accountId]);
 async function download(){const version=active.current;setExporting(true);setExportError('');try{const bytes=await api<ArrayBuffer>('/teaching/student/me/enrollments/'+enrollmentId+'/grading-result/export',{responseType:'bytes'});if(version===active.current)await shareDocument(bytes,'bang-diem-'+enrollmentId+'.csv','text/csv');}catch{if(version===active.current)setExportError('Chưa tải được bảng điểm. Kiểm tra kết nối hoặc tải lại kết quả công bố.');}finally{if(version===active.current)setExporting(false);}}
 const query=useStudentQuery<Published>('/teaching/student/me/enrollments/'+enrollmentId+'/grading-result');const data=query.data;
 useEffect(()=>{onPublishedChange(data?.state==='PUBLISHED'&&!!data.result||data?.state==='UPDATING');},[data,onPublishedChange]);
 useEffect(()=>{if(refreshRevision>0)query.reload();},[refreshRevision,query.reload]);
 if(query.loading)return <Card><Text style={textStyles.body}>Đang kiểm tra kết quả đánh giá…</Text></Card>;
 if(query.error)return <Card><InlineNotice title="Chưa tải được kết quả đánh giá" action={{title:'Thử lại',onPress:query.reload}}/></Card>;
 if(!data||data.state!=='PUBLISHED'||!data.result)return data?.state==='UPDATING'?<Card><InlineNotice title="Kết quả theo quy chế đang được trung tâm cập nhật"/></Card>:null;
 const row=data.result;
 return <Card><Heading title="Kết quả đánh giá đã công bố"/><Text style={textStyles.label}>{data.templateName}</Text>{data.publishedAt&&<Text style={textStyles.body}>Công bố {displayDate(data.publishedAt.slice(0,10))} · phiên bản {data.revision}</Text>}
 {query.refreshError&&<InlineNotice tone="warning" title="Đang xem bản đã lưu; chưa xác minh được cập nhật mới nhất" action={{title:'Tải lại',onPress:query.reload}}/>}
 {row.score!==null&&<Text style={[textStyles.section,{fontSize:28,lineHeight:36}]}>{number(row.score)} {row.unit==='PERCENT'?'%':row.unit==='BAND'?'band':'điểm TOEIC'}</Text>}
 <View style={{gap:8}}><Text style={textStyles.label}>Học thuật</Text><Badge label={labels[row.academicStatus]??row.academicStatus} tone={row.academicStatus==='PASS'?'success':'info'}/><Text style={textStyles.label}>Điều kiện hoàn thành</Text><Badge label={labels[row.completionStatus]??row.completionStatus} tone={row.completionStatus==='PASS'?'success':'info'}/></View>
 {row.attendance.status!=='NOT_REQUIRED'&&<Text style={textStyles.body}>Chuyên cần: {row.attendance.percent===null?'Chưa đủ dữ liệu':number(row.attendance.percent)+'%'}</Text>}
 {row.reasons.map((r,i)=><Text key={i} style={textStyles.body}>{r}</Text>)}
 {row.components.map(part=><Disclosure key={part.code} title={part.name+' · '+(part.level?labels[part.level]:part.score===null?labels[part.status]??part.status:number(part.score)+' / '+number(part.maxScore))}><View style={{gap:10}}>{part.assessments.map(item=><View key={item.rootId} style={{gap:4}}><Text style={textStyles.label}>{item.name}</Text><Text style={textStyles.body}>{displayDate(item.date)} · {labels[item.status]??item.status}{item.score!==null?' · '+number(item.score):''}</Text>{item.rawScore!==null&&<Text style={textStyles.body}>{item.rawScore}/100 câu đúng · bảng quy đổi #{item.conversionId}</Text>}{item.comment&&<Text style={textStyles.body}>{item.comment}</Text>}{item.history&&item.history.length>1?<Disclosure title={'Lịch sử '+item.history.length+' lần đánh giá'}>{item.history.map(h=><View key={h.activityId} style={{gap:4,marginBottom:12}}><Text style={textStyles.label}>{h.name}{h.selected?' · Đang tính vào tổng':''}</Text><Text style={textStyles.body}>{displayDate(h.date)} · {labels[h.status]??h.status}{h.level?' · '+(labels[h.level]??h.level):h.score!==null?' · '+number(h.score):''}</Text>{h.rawScore!==null&&<Text style={textStyles.body}>{h.rawScore}/100 câu đúng · bảng #{h.conversionId}</Text>}{h.comment&&<Text style={textStyles.body}>{h.comment}</Text>}</View>)}</Disclosure>:item.attempts.length>1&&<Text style={textStyles.body}>Bản công bố này ghi nhận {item.attempts.length} lần đánh giá, chưa kèm chi tiết từng lần.</Text>}</View>)}</View></Disclosure>)}
 {exportError&&<InlineNotice tone="warning" title={exportError}/>}<Button title={exporting?'Đang tải bảng điểm…':'Tải / chia sẻ bảng điểm CSV'} secondary disabled={exporting} onPress={()=>void download()}/>
 <Disclosure title="Cách tính và lưu ý">{data.rules.map((r,i)=><Text key={i} style={textStyles.body}>{r}</Text>)}{(row.unit==='BAND'||row.unit==='TOEIC')&&<Text style={textStyles.body}>Kết quả thi thử tại trung tâm, không thay thế chứng chỉ chính thức.</Text>}</Disclosure>
 </Card>;
}
